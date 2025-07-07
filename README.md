# CodeSphere - Collaborative Code Editor

**CodeSphere** is a full-stack collaborative code editor that allows developers to write, run, and manage code together in real-time. Built for distributed teams, classrooms, and hackathons, it combines live code editing, execution, chat, and GitHub integration — all in the browser.


## 🚀 Features

- 🔐 **GitHub OAuth Login** – Secure user authentication with GitHub.
- 📁 **Project & File Management** – Create projects, manage files/folders, and collaborate with others.
- 🧑‍💻 **Real-Time Collaboration** – Live code editing synced across users via WebSockets.
- 💬 **Integrated Chat** – Chat with team members inside the coding workspace.
- ⚙️ **Multi-Language Support** – Write and execute code in Java, Python, and C++ using the Judge0 API.
- ☁️ **GitHub Integration** – Create repositories, commit code, and track version history directly from the editor.
- 💾 **MongoDB & Redis** – Persistent data storage with Redis handling session management.


## 🛠️ Tech Stack

**Frontend:**
- React.js
- Tailwind CSS
- Monaco Editor
- SockJS & STOMP (for WebSocket communication)

**Backend:**
- Spring Boot (Java)
- WebSockets (STOMP)
- Redis (session & chat storage)
- MongoDB (user & project data)
- Judge0 API (code execution)
- GitHub OAuth2 & GitHub API

## 📁 Repository Structure

    .
    ├── collaborative-code-editor-frontend   # React-based frontend
    └── collaborative-code-editor-backend    # Spring Boot backend


## 🧪 How to Run Locally

### Prerequisites

- Node.js & npm
- Java 17+
- Redis & MongoDB
- GitHub OAuth App credentials
- Judge0 API access (you can use a public instance for testing)


### Backend Setup

```bash
cd collaborative-code-editor-backend
# Configure `application.yml` with:
# - GitHub client ID & secret
# - Redis URI
# - MongoDB URI
# - Judge0 API endpoint
./mvnw spring-boot:run
```

### Frontend Setup
```bash
cd collaborative-code-editor-frontend
npm install
npm start
```

The frontend will run at:
http://localhost:3000

The backend server should be running at:
http://localhost:8080

## 🎯 Use Cases
- Remote pair programming
- Hackathons & coding competitions
- Group academic projects
- Real-time coding interviews
- Collaborative learning environments

## 📄 License
This project is licensed under the MIT License.

## 🤝 Let's Collaborate
Have ideas or improvements? Feel free to fork, contribute, or get in touch — collaboration is always welcome!