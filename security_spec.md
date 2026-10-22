# Security Specification for আড্ডা (Adda)

## 1. Data Invariants
- Users can only read and write their own profile document (`/users/{userId}` where `userId == request.auth.uid`), except for reading other registered profiles to display the People directory ("মানুষ").
- Direct chats can only be read, created, or updated by their 2 participants (`request.auth.uid in resource.data.participantIds`).
- Groups can only be read by their members or if the group is the public default "সবাই" group (`resource.data.isDefaultPublic == true`).
- New group messages can only be sent by authenticated members, and must include `senderId == request.auth.uid`.
- Messages can only be deleted by the original sender (`resource.data.senderId == request.auth.uid`).
- Calls can only be created by the initiator (`initiatorId == request.auth.uid`), and participants can join or end the call.

## 2. Dirty Dozen Payloads & Test Scenarios
1. Unauthenticated user attempts to read `/users`. (Must fail)
2. User Alice attempts to overwrite Bob's profile `/users/bob_456`. (Must fail)
3. User Alice queries messages in a private direct chat where she is not a participant. (Must fail)
4. User attempts to inject a 50KB string as an ID or name. (Must fail)
5. User attempts to modify `creatorId` on an existing group. (Must fail)
6. Non-member attempts to read a private group document. (Must fail)
7. Non-sender attempts to delete another user's message. (Must fail)
8. User creates a call with another user's UID as `initiatorId`. (Must fail)
9. User updates `createdAt` timestamp on an existing user profile or group. (Must fail)
10. Member updates message text of another sender. (Must fail)
11. Participant updates `seenBy` array on a message they received. (Must succeed)
12. Authenticated user joins public "সবাই" group by updating their presence or member list. (Must succeed)
