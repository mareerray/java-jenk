# SonarQube Dashboard Navigation Guide
## 1. Main Entry Points (Top Menu Bar)
​
The main navigation tabs in SonarQube are:

- Projects - Overview of all your analyzed projects

- Issues - Global view of all code issues across projects

- Rules - Code quality rules and their definitions

- Quality Profiles - Rulesets used for analysis

- Quality Gates - Pass/Fail criteria for builds

- Administration - User and system settings

## 2. Projects Dashboard
​
This is your starting point. It shows:

### Left Sidebar - Filters:
- Quality Gate: Shows how many projects Passed vs Failed

- Security: Filters issues by security severity (A=0 issues, B=≥1 low, C=≥1 medium, D=≥1 high, E=≥1 blocker)

- Reliability: Same severity levels for reliability issues

- Maintainability: Estimated effort to fix all issues

### Main Panel - Project Cards:
Each project card displays:

- Project Name with a star (favorite indicator)

- Status badge (Passed ✓ or Failed ✗)

- Last analysis timestamp

- Language metrics (Lines of Code, language types)

- Four key metrics:

    - Security - Vulnerabilities found

    - Reliability - Bugs that could cause failures

    - Maintainability - Code complexity issues

    - Coverage - Test code coverage percentage

### For your SafeZone project:

- All backend services show Passed ✓ quality gates

- Frontend shows 22 Reliability issues and 127 Maintainability issues

## 3. Individual Project Dashboard
​
When you click on a project (e.g., "Safe Zone - Discovery Service"), you see:

### Overview Tab:
- Quality Gate Status - Large green checkmark = PASSED

- New Code vs Overall Code tabs - Compare recent changes vs entire codebase

- Key Metrics:

    - New issues (in the last period)

    - Accepted issues (acknowledged but not fixed)

    - Coverage (test code coverage)

    - Duplications (duplicated code blocks)

    - Security Hotspots (potential security risks)

### Key Information:

- Passed = Quality gate passed (meets your defined standards)

- Last analysis 17 hours ago = When SonarQube last scanned this project

- 49 Lines of Code = Project size

- Version not provided = No specific version tag

## 4. Issues Tab
​
This tab shows detailed code quality issues:

### Left Sidebar - Filtering:
- My Issues / All - Toggle between your assigned issues and all issues

- Issues in new code - Filter for recent changes only

- Software Quality categories:

    - Security - 0 issues

    - Reliability - 0 issues

    - Maintainability - 1 issue (in this project)

- Severity levels:

    - Blocker (0) - Must fix before release

    - High (1) - Important to fix

    - Medium (0) - Should fix

    - Low (0) - Nice to fix

    - Info (0) - Informational

### Main Panel - Individual Issues:
Each issue shows:

- File location - Where the problem is

- Issue description - What the problem is

- Category badge (Maintenance, Security, etc.)

- Severity level (with color: red=high, orange=medium, etc.)

- Assignment status - Who should fix it

- Effort - Estimated time to fix

- Creation date - When issue was first detected

## 5. Critical Areas to Monitor
​
Based on your current SafeZone project:

### Global Issues View:
- Click "Issues" in the top menu to see all security issues across ALL projects

- Your current critical issue: MongoDB password exposed (9 blocker issues total)

- Filtering by severity: Click on "E ≥ 1 blocker issue" to see blocking problems

```css
What to look for:

1. Security (Red E badges) - Password/key exposure, SQL injection risks

2. Reliability (Red D badges) - Logic errors, potential crashes

3. Maintainability (Yellow C badges) - Code complexity, duplications

4. Coverage - Should be high (aim for >70%)

5. Quality Gates - Must all show green checkmarks

````

## 6. Your Current Status
✅ Passing Quality Gates:

- Safe Zone - Discovery Service

- Safe Zone - Gateway Service

- Safe Zone - Media Service

- Safe Zone - Frontend

⚠️ Needs Attention:

- MongoDB Password Exposure (BLOCKER) - 9 issues

- Frontend: 22 Reliability issues + 127 Maintainability issues

These are preventing code quality improvements

## 7. Action Items from Dashboard
1. View all blockers → Issues menu → Filter by "Blocker" severity

2. See security hotspots → Click "Security Hotspots" tab on any project

3. Track fixes → Issues can be marked "Open", "Resolved", or "False Positive"

4. Monitor trends → Activity section shows issue progression over time

5. Check Quality Gates → Quality Gates tab shows pass/fail rules

The key is: Start at Projects dashboard for overview, then drill into specific issues when needed.