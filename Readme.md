---

## 🖼️ Example Run (Proof of Successful Execution)

Below is a screenshot showing the output after authenticating with Dropbox Business and fetching team members via the API endpoint:

**Endpoint used:**
`https://api.dropboxapi.com/2/team/members/list_v2`

**Result:**
Successfully retrieved user profile information, including email, name, membership type, and status.

![](C:\Users\26adi\OneDrive\Pictures\Screenshots 1\Screenshot 2025-10-18 041612.png)

src="C:\Users\26adi\OneDrive\Pictures\Screenshots 1\Screenshot 2025-10-18 041612.png" width="1919"/>

**Sample JSON Output:**
```json
{
"members": [
{
"profile": {
"team_member_id": "dbmid:AAAUxgIDYoWFHgbUSl9UqiVRINEjau7bFU",
"account_id": "dbid:AACKySKVr92yo3t6en60nd1Buj-4Lt5_iIk",
"email": "adityagupta.bbh@gmail.com",
"email_verified": true,
"status": { "tag": "active" },
"name": {
"given_name": "Aditya",<img height="1147" 
"surname": "Gupta",
"display_name": "Aditya Gupta",
"abbreviated_name": "AG"
},
"membership_type": { "tag": "full" },
"joined_on": "2025-10-17T19:31:02Z"
}
}
],
"has_more": false
}
