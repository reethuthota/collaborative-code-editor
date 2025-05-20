package com.collaborative_code_editor.service;

import com.collaborative_code_editor.model.WebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void storeUserSession(String githubLogin, Map<String, Object> userDetails, long duration, TimeUnit unit) {
        redisTemplate.opsForValue().set(githubLogin, userDetails, duration, unit);
    }

    public void removeUserSession(String githubLogin) {
        redisTemplate.delete(githubLogin);
    }

    public void addUserToProject(String projectId, String userId) {
        redisTemplate.opsForSet().add("users:" + projectId, userId);
    }

    public void removeUserFromProject(String projectId, String userId) {
        redisTemplate.opsForSet().remove("users:" + projectId, userId);
    }

    public Set<Object> getProjectUsers(String projectId) {
        return redisTemplate.opsForSet().members("users:" + projectId);
    }

    public void removeUserAndCleanUpIfLast(String projectId, String userId) {
        String userKey = "users:" + projectId;
        Long userCount = redisTemplate.opsForSet().size(userKey);

        if (userCount != null && userCount <= 1) {
            // Remove the last user
            redisTemplate.opsForSet().remove(userKey, userId);

            // Clean up related Redis keys
            redisTemplate.delete("chat:" + projectId);
            redisTemplate.delete("typing:" + projectId);

            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.addAll(redisTemplate.keys("code:" + projectId + ":*"));
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } else {
            // Just remove the user
            redisTemplate.opsForSet().remove(userKey, userId);
        }
    }


    // Live Collaboration State
    public void updateUserEditingState(String projectId, String userId, String cursor, String tempCode) {
        String key = "collab:project:" + projectId + ":user:" + userId;
        redisTemplate.opsForHash().put(key, "cursor", cursor);
        redisTemplate.opsForHash().put(key, "code", tempCode);
    }

    public Map<Object, Object> getUserEditingState(String projectId, String userId) {
        String key = "collab:project:" + projectId + ":user:" + userId;
        return redisTemplate.opsForHash().entries(key);
    }

    // WebSocket Sessions
    public void addWebSocketClient(String socketId, String userId, String projectId) {
        redisTemplate.opsForSet().add("ws:clients", socketId);
        redisTemplate.opsForHash().put("ws:session:" + socketId, "userId", userId);
        redisTemplate.opsForHash().put("ws:session:" + socketId, "projectId", projectId);
    }

    public void removeWebSocketClient(String socketId) {
        redisTemplate.opsForSet().remove("ws:clients", socketId);
        redisTemplate.delete("ws:session:" + socketId);
    }

    public WebSocketClient getWebSocketClient(String socketId) {
        Map<Object, Object> metadata = redisTemplate.opsForHash().entries("ws:session:" + socketId);
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        String userId = (String) metadata.get("userId");
        String projectId = (String) metadata.get("projectId");
        if (userId == null || projectId == null) {
            return null;
        }
        return new WebSocketClient(userId, projectId);
    }

    public Map<Object, Object> getClientMetadata(String socketId) {
        return redisTemplate.opsForHash().entries("ws:session:" + socketId);
    }

    public void storeCurrentCode(String projectId, String filePath, String code) {
        String key = "code:" + projectId + ":" + filePath;
        redisTemplate.opsForValue().set(key, code);
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    public String getCurrentCode(String projectId, String filePath) {
        try {
            String key = "code:" + projectId + ":" + filePath;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteCode(String projectId, String filePath) {
        redisTemplate.delete("code:" + projectId + ":" + filePath);
    }

    public void deleteChatAndTyping(String projectId, String filePath) {
        redisTemplate.delete("chat:" + projectId);
        redisTemplate.delete("typing:" + projectId);
    }

    // General data persistence
    public void storeData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String retrieveData(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void storeInitialFileTree(String projectId, List<Map<String, Object>> fileTree) {
        String key = "structure:" + projectId;
        redisTemplate.opsForValue().set(key, fileTree, Duration.ofHours(1));
    }

    public void storeFileStructure(String projectId, Map<String, Object> newItem, String parentPath) {
        String key = "structure:" + projectId;
        List<Map<String, Object>> fileTree = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
        if (fileTree == null) {
            fileTree = new ArrayList<>();
        } else {
            fileTree = deepCopyFileTree(fileTree);
        }

        Map<String, Object> item = new HashMap<>();
        item.put("name", newItem.get("name"));
        item.put("type", newItem.get("type"));
        item.put("path", newItem.get("path"));
        if ("file".equals(newItem.get("type"))) {
            item.put("content", newItem.get("content") != null ? newItem.get("content") : "");
            item.put("originalContent", newItem.get("originalContent") != null ? newItem.get("originalContent") : "");
        } else if ("folder".equals(newItem.get("type"))) {
            item.put("children", new ArrayList<>());
        }

        if (parentPath == null || parentPath.isEmpty()) {
            fileTree.add(item);
        } else {
            Map<String, Object> parent = findParentInTree(fileTree, parentPath);
            if (parent != null) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.computeIfAbsent("children", k -> new ArrayList<>());
                if (!children.stream().anyMatch(child -> newItem.get("name").equals(child.get("name")))) {
                    children.add(item);
                }
            }
        }

        redisTemplate.opsForValue().set(key, fileTree, Duration.ofHours(1));
    }

    public void deleteFileStructure(String projectId, String path) {
        String key = "structure:" + projectId;
        List<Map<String, Object>> fileTree = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
        if (fileTree == null) {
            return;
        }
        fileTree = deepCopyFileTree(fileTree); // Create a deep copy to avoid modifying cached object

        String parentPath = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
        if (parentPath.isEmpty()) {
            fileTree.removeIf(item -> path.equals(item.get("path")));
        } else {
            Map<String, Object> parent = findParentInTree(fileTree, parentPath);
            if (parent != null) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                if (children != null) {
                    children.removeIf(child -> path.equals(child.get("path")));
                }
            }
        }

        redisTemplate.opsForValue().set(key, fileTree, Duration.ofHours(1));
    }

    private List<Map<String, Object>> deepCopyFileTree(List<Map<String, Object>> fileTree) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> item : fileTree) {
            Map<String, Object> itemCopy = new HashMap<>(item);
            if ("folder".equals(item.get("type")) && item.get("children") != null) {
                itemCopy.put("children", deepCopyFileTree((List<Map<String, Object>>) item.get("children")));
            }
            copy.add(itemCopy);
        }
        return copy;
    }

    public Map<String, Object> findParentInTree(List<Map<String, Object>> fileTree, String parentPath) {
        for (Map<String, Object> item : fileTree) {
            if (parentPath.equals(item.get("path"))) {
                return item;
            }
            if ("folder".equals(item.get("type"))) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) item.get("children");
                if (children != null) {
                    Map<String, Object> found = findParentInTree(children, parentPath);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }
}

