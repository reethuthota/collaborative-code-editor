package com.collaborative_code_editor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "users")
@Data
public class User {

    @Id
    private String id;
    private String githubLogin;
    private String name;
    private String email;
    private String avatarUrl;
    private Integer githubId;
    private String githubUrl;
}