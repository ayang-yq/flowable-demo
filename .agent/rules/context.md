---
trigger: always_on
---

# Shell Environment & Executive Rules
- **Shell Preference**: Always use **PowerShell 7.x** (pwsh) for executing terminal commands. Use PowerShell-compatible syntax (e.g., avoid bash-specific redirects or pipes that pwsh handles differently).
- if base64 command needed, use [System.Convert]::ToBase64String() instead. eg. curl.exe -X GET "http://localhost:8080/api/cases/statistics" -H "Authorization: Basic $([Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes('admin:admin')))"

# Pre-start Port Checking & Auto-Recovery
Before starting any service, you must perform a port check and automatically clear any existing processes to ensure a clean start.

## Backend Service (Port 8080)
1. **Check**: Before executing the backend start command, run: 
   `netstat -ano | findstr :8080`
2. **Auto-Kill**: If a process is detected on port 8080, execute the following command immediately without asking for permission:
   `taskkill /F /PID {process-id}`
3. **Action**: Proceed to start the backend service only after ensuring the port is clear.

## Frontend Service (Port 3000)
1. **Check**: Before executing the frontend start command, run: 
   `netstat -ano | findstr :3000`
2. **Auto-Kill**: If a process is detected on port 3000, execute the following command immediately without asking for permission:
   `taskkill /F /PID {process-id}`
3. **Action**: Proceed to start the frontend service only after ensuring the port is clear.

# Workflow Requirements
- **No Confirmation Needed**: You are authorized to kill processes on the specified ports (8080, 3000) automatically to resolve "Port already in use" errors.
- **Silent Operation**: Use `-ErrorAction SilentlyContinue` to prevent the workflow from breaking if a port is already empty.

# Document
Always keep README.md up to date.
