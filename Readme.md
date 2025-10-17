---

## 🖼️ Example Run (Proof of Successful Execution)

Below is a screenshot showing the output after authenticating with Dropbox Business and fetching team members via the API endpoint:

**Endpoint used:**
`https://api.dropboxapi.com/2/team/members/list_v2`

**Result:**
Successfully retrieved user profile information, including email, name, membership type, and status.

**Sample JSON Output:**
```json
{
"members": [
{
"profile": {
"team_member_id": "dbmid:AAAUxgIDYoWFHgbUSI9UqiVIRINEjau7bFU",
"account_id": "dbid:AACKySKVr92yo9t3en60nd1Buj-4Lt5_iIk",
"email": "adityagupta.bhl@gmail.com",
"email_verified": true,
"secondary_emails": [],
"status": {
".tag": "active"
},
"name": {
"given_name": "Aditya",
"surname": "Gupta",
"familiar_name": "Aditya",
"display_name": "Aditya Gupta",
"abbreviated_name": "AG"
},
"membership_type": {
".tag": "full"
},
"joined_on": "2025-10-17T19:31:00Z",
"groups": [
"g:3c7fa6ba1a4ed2340000000000000003"
],
"member_folder_id": "11919298355",
"root_folder_id": "13044157633"
},
"roles": [
{
"role_id": "pid_dbtmr:AAAAAFMcx6E0tax39Kb0H671TzWLeE07dwaqFQ5fDRy2",
"name": "Team",
"description": "Manage everything and access all permissions"
}
]
},
{
"profile": {
"team_member_id": "dbmid:AACxMDiqEymUkemzhmp7fa_Y_t-haq7vnLc",
"account_id": "dbid:AAAyLVg1ZBtabGzv9AUuqkbYTHdNvVCqEm8",
"email": "26adityagupta.bhl@gmail.com",
"email_verified": false,
"secondary_emails": [],
"status": {
".tag": "invited"
},
"name": {
"given_name": "",
"surname": "",
"familiar_name": "",
"display_name": "",
"abbreviated_name": ""
},
"membership_type": {
".tag": "full"
},
"invited_on": "2025-10-17T19:33:13Z",
"groups": [
"g:3c7fa6ba1a4ed2340000000000000003"
],
"member_folder_id": "13041698001",
"root_folder_id": "13031102401"
}
}
],
"cursor": "AABNk5dl6ln1UqALrC4qKs9kea6aRjSGOa9j5oofjiSvbp7PqipclCa4WWIdnPN2KDSWy7PKdvNsV7q11hoDVqn-DlWeiFc1_J0T7q4db6ywjg",
"has_more": false
}
