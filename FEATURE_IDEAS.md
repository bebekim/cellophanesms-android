# Feature Ideas for CellophaneSMS

Feature ideas gathered from analyzing open-source SMS apps:
- [QUIK](https://github.com/octoshrimpy/quik)
- [Simple SMS Messenger](https://github.com/SimpleMobileTools/Simple-SMS-Messenger)
- [DekuSMS](https://github.com/dekusms/DekuSMS-Android)

---

## From QUIK

- [ ] Scheduled/delayed message sending
- [ ] Swipe actions on conversations (archive, delete, pin)
- [ ] Emoji reactions to messages
- [ ] Voice message support
- [ ] Speech-to-text and text-to-speech
- [ ] Quick-reply from notifications
- [ ] File attachments

## From Simple SMS Messenger

- [ ] Conversation export/import (backup/restore)
- [ ] Configurable lockscreen preview (privacy control)
- [ ] Customizable fonts and themes
- [ ] Message search functionality
- [ ] Contact blocking (Android 7+)
- [ ] Adjustable date/time format options

## From DekuSMS

- [ ] End-to-end encryption between DekuSMS users
- [ ] Cloud message forwarding (webhook-style to custom servers)
- [ ] RabbitMQ gateway for external integrations
- [ ] Perfect Forward Secrecy (Double Ratchet algorithm)
- [ ] Message queue with retry logic for reliability
- [ ] Reproducible builds for security verification

---

## Architectural Improvements

- [ ] Extract SMS/MMS handling into separate reusable module (`sms-core/`)
- [ ] Isolate encryption code into dedicated module (`crypto/`)
- [ ] Add Fastlane for automated F-Droid/Play Store releases
- [ ] Consider git submodules for cryptographic libraries
- [ ] Implement reproducible builds for user trust

---

## Priority Suggestions

### High Priority (Core UX)
1. Scheduled message sending
2. Conversation backup/export
3. Swipe actions on threads
4. Message search

### Medium Priority (Enhanced Features)
5. Quick-reply improvements
6. Lockscreen privacy controls
7. Customizable themes
8. Message queue with retry for API calls

### Low Priority (Advanced)
9. E2E encryption between app users
10. Cloud forwarding/webhooks
11. Gateway functionality
