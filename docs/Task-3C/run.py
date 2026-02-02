
import os
import glob
import subprocess
import requests
import google.generativeai as genai
import time

# --- Configuration ---
# Ensure these environment variables are set before running the script
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")
REPO_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../"))
DOCS_PATH = os.path.dirname(__file__)
REPORT_FILE = os.path.join(DOCS_PATH, "smells_and_refactored.md")
BRANCH_NAME = "Agentic_Pipeline"
BASE_BRANCH = "main"  # Adjust if your default branch is master

# --- Setup Gemini ---
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)
    model = genai.GenerativeModel('gemini-1.5-flash') # Using Flash for speed/cost efficiently
else:
    print("WARNING: GEMINI_API_KEY not found. LLM features will be disabled.")
    model = None

def run_command(command, cwd=REPO_PATH):
    """Runs a shell command and returns output."""
    try:
        result = subprocess.run(
            command, cwd=cwd, shell=True, check=True, capture_output=True, text=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command '{command}': {e.stderr}")
        raise

def get_java_files(root_dir):
    """Recursively finds all .java files in the given directory."""
    java_files = []
    # Adjust path to point to source root. Assuming standard structure based on file list.
    src_path = os.path.join(root_dir, "app", "src", "main", "java")
    if not os.path.exists(src_path):
        print(f"Source path {src_path} not found. Searching valid subdirectories...")
        # Fallback to recursively searching the whole repo if specific structure missing
        src_path = root_dir

    for dirpath, _, filenames in os.walk(src_path):
        for filename in filenames:
            if filename.endswith(".java"):
                java_files.append(os.path.join(dirpath, filename))
    return java_files

def analyze_code(file_path, code_content):
    """Uses Gemini to identify smells and suggest refactoring."""
    if not model:
        return "## Analysis Skipped\nMissing API Key."

    prompt = f"""
    You are an expert software architect. Analyze the following Java code for common design smells (e.g., God Class, Long Method, Feature Envy, Duplication).
    
    File: {os.path.basename(file_path)}

    Code:
    ```java
    {code_content}
    ```

    Task:
    1. Identify specific design smells.
    2. Provide a refactored version of the code that fixes these smells.
    3. Explain the changes.

    Output Format (Markdown):
    ## File: {os.path.basename(file_path)}
    ### Identified Smells
    - [Smell Name]: Description...
    ### Refactored Code
    ```java
    // Refactored code here
    ```
    ### Explanation
    ...
    """
    
    try:
        response = model.generate_content(prompt)
        return response.text
    except Exception as e:
        return f"## Error Analyzing {os.path.basename(file_path)}\n{str(e)}"

def get_repo_info():
    """Extracts owner and repo name from git remote."""
    remote_url = run_command("git remote get-url origin")
    # Handle SSH and HTTPS urls
    # SSH: git@github.com:owner/repo.git
    # HTTPS: https://github.com/owner/repo.git
    if "github.com" not in remote_url:
        print("Not a GitHub repo or origin not set to GitHub.")
        return None, None
    
    parts = remote_url.strip().replace(".git", "").split("/")
    repo = parts[-1]
    owner = parts[-2].split(":")[-1]
    return owner, repo

def create_pull_request(owner, repo, title, body, head, base):
    """Creates a Pull Request using GitHub API."""
    if not GITHUB_TOKEN:
        print("GITHUB_TOKEN not found. Skipping PR creation.")
        print(f"Please manually create a PR from branch '{head}' to '{base}'.")
        return

    url = f"https://api.github.com/repos/{owner}/{repo}/pulls"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    data = {
        "title": title,
        "body": body,
        "head": head,
        "base": base
    }
    
    response = requests.post(url, json=data, headers=headers)
    if response.status_code == 201:
        print(f"Pull Request created successfully: {response.json()['html_url']}")
    else:
        print(f"Failed to create PR: {response.status_code}")
        print(response.json())

def main():
    print("Starting Agentic Refactoring Pipeline...")

    # 1. Setup Branch
    print(f"Setting up branch '{BRANCH_NAME}'...")
    run_command(f"git checkout {BASE_BRANCH}")
    run_command("git pull origin " + BASE_BRANCH)
    run_command(f"git checkout -b {BRANCH_NAME} || git checkout {BRANCH_NAME}")

    # 2. Find Files
    files = get_java_files(REPO_PATH)
    print(f"Found {len(files)} Java files.")
    
    # Process a subset for demonstration/rate-limits if needed
    # files = files[:5] 

    report_content = "# Agentic Refactoring Report\n\nGenerated by Gemini-powered pipeline.\n\n"

    # 3. Analyze Loop
    for i, file_path in enumerate(files):
        print(f"Analyzing [{i+1}/{len(files)}]: {os.path.basename(file_path)}")
        try:
            with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            
            # Skip very small files
            if len(content.strip()) < 100:
                continue

            analysis = analyze_code(file_path, content)
            report_content += analysis + "\n\n---\n\n"
            
            # Rate limiting
            time.sleep(2) 
            
        except Exception as e:
            print(f"Failed to process {file_path}: {e}")

    # 4. Write Report
    print(f"Writing report to {REPORT_FILE}...")
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write(report_content)

    # 5. Commit and Push
    print("Committing changes...")
    try:
        run_command(f"git add {REPORT_FILE}")
        run_command('git commit -m "chore: Generate refactoring suggestions report" || echo "No changes to commit"')
        run_command(f"git push -u origin {BRANCH_NAME}")
    except Exception as e:
        print(f"Git operation failed: {e}")
        return

    # 6. Create PR
    print("Creating Pull Request...")
    owner, repo = get_repo_info()
    if owner and repo:
        create_pull_request(
            owner, repo,
            title="[Agentic] Refactoring Suggestions",
            body="This PR contains a report of identified design smells and suggested refactorings generated by the Agentic Pipeline.",
            head=BRANCH_NAME,
            base=BASE_BRANCH
        )
    else:
        print("Could not determine repo info for PR creation.")

if __name__ == "__main__":
    main()
