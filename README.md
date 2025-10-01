# Python Executor

A feature-rich, cross-platform desktop application built with Java Swing for writing, managing, and executing Python scripts. It provides an integrated development environment with a modern UI, a powerful code editor, file management capabilities, and built-in Git support.


*(Replace this with a screenshot of your application)*

---

## ✨ Core Features

*   **Modern UI & UX:**
    *   Sleek dark theme inspired by modern code editors like VS Code.
    *   Toggleable panels for a customizable, distraction-free workspace.
    *   Global file search to instantly find scripts and input files.
    *   "Recent Folders" menu for quick project switching.

*   **📂 Integrated File Explorer:**
    *   Switch between a classic dropdown view and a full file explorer tree.
    *   **Create, Rename, Move, and Delete** files and folders directly from the UI.
    *   Right-click context menus for intuitive file operations.

*   **✍️ Advanced Code Editor:**
    *   Rich **Python syntax highlighting** powered by `RSyntaxTextArea`.
    *   **Code folding** for navigating large files.
    *   **Automatic, Python-aware indentation** (handles colons and tab levels).
    *   **Error Highlighting:** Parses Python tracebacks and draws a squiggly red underline on the exact line that caused an error.
    *   **Find & Replace** functionality.
    *   Full **Undo/Redo** support.

*   **🚀 Script Execution:**
    *   Run Python scripts with a single click or keyboard shortcut (`Shift + Enter`).
    *   Provide script input directly from a dedicated text area or by loading an input file.
    *   View script output and errors in a clear, timestamped log panel.

*   **🔧 Git Integration:**
    *   A dedicated Version Control panel for common Git commands.
    *   Interactively **Add** files to staging with a visual file selector.
    *   **Commit** staged files with an in-app message editor.
    *   **Pull** from and **Push** to remote repositories with real-time output.
    *   Quickly open **Git Bash** in your project's root or view the **Git Log**.

*   **⚙️ Customization:**
    *   Adjust editor **font size** and **tab size** on the fly.
    *   Easily reset folder selections or reload the application window from the Settings panel.

## 📋 Requirements

To build and run this application, you will need:
1.  **Java JDK 17** or newer.
2.  **Apache Maven** to build the project.
3.  **Python 3.x** installed and available in your system's PATH.
4.  **Git** installed and available in your system's PATH for version control features.

## 🚀 How to Build and Run

1.  **Clone the repository:**
    ```sh
    git clone <your-repository-url>
    cd executor
    ```

2.  **Build the application using Maven:**
    This command will compile the source code and package it into a single, executable JAR file with all dependencies included.
    ```sh
    mvn clean package
    ```

3.  **Run the application:**
    The executable JAR will be created in the `target/` directory.
    ```sh
    java -jar target/executor-2.1.1-SNAPSHOT-jar-with-dependencies.jar
    ```

## ⌨️ Keyboard Shortcuts

The application is designed to be fully navigable and usable via the keyboard.

| Action                   | Shortcut(s)                               |
| ------------------------ | ----------------------------------------- |
| **File & Navigation**    |                                           |
| Select Script Folder     | `Ctrl + Shift + S`                        |
| Select Input Folder      | `Ctrl + Shift + I`                        |
| Refresh File Lists       | `Ctrl + Shift + R`                        |
| Toggle Settings Panel    | `Ctrl + ,`                                |
| **Editor & Execution**   |                                           |
| Run Script               | `Shift + Enter`                           |
| Save Script              | `Ctrl + S`                                |
| Undo                     | `Ctrl + Z`                                |
| Redo                     | `Ctrl + Y`                                |
| Enable/Toggle Editing    | `Ctrl + E`                                |
| Increase Font Size       | `Ctrl + =` / `Ctrl + Numpad +`            |
| Decrease Font Size       | `Ctrl + -` / `Ctrl + Numpad -`            |
| **UI & Panels**          |                                           |
| Toggle Output Panel      | `Ctrl + J`                                |
| Toggle Control Panels    | `Ctrl + H`                                |
| Toggle Input Panel       | `Ctrl + I`                                |
| **Git Version Control**  |                                           |
| Git Add                  | `Ctrl + Shift + A`                        |
| Git Commit               | `Ctrl + Shift + C`                        |
| Git Pull                 | `Ctrl + Shift + U`                        |
| Git Push                 | `Ctrl + Shift + P`                        |
| Git Log                  | `Ctrl + Shift + L`                        |
| Open Git Bash            | `Ctrl + Alt + B`                          |

---

## ⚖️ License

This project is licensed under the MIT License. See the `LICENSE` file for details.
