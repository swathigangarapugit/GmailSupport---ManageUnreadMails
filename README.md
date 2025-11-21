# GmailSupport---ManageUnreadMails
This project creates an agent to handle gmail account which has plenty of unread mails and categorizes them based on priority

## Problem
Everyone has a dead Gmail with 100k+ unread emails.

## Magic Solution
Say "rescue my gmail" → 5-agent swarm cleans everything, unsubscribes, creates your 10-year life PDF.

## Demo Video (2:48)
https://youtu.be/xxxxxx

## 10 Required Features Used
1. Multi-agent system (5 agents)
2. Parallel + Sequential + Loop agents
3. Gemini 1.5 Pro powered
4. Tools (Gmail API OpenAPI + custom PDF)
5. Long-running pause/resume (45s unsubscribe)
6. Sessions & Memory + Vector MemoryBank (life events)
7. Observability (OpenTelemetry traces)
8. A2A Protocol (agents debate archiving mom’s emails)
9. Agent deployment ready (Docker + Cloud Run)
10. Real Gmail OAuth

Run: mvn package → java -jar target/gmail-life-support-1.0.jar → type "rescue my gmail"

Built 100% with Google Java ADK + Gemini + Gmail API
