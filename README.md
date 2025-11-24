# Gmail Rescue — Smart Inbox Intelligence & Life Story Generator
Turn your noisy Gmail inbox into meaningful insights using powerful AI agents.

## Overview
Gmail Rescue is a multi-agent Gmail assistant that summarizes inbox activity, cleans up email clutter, and even reconstructs a personal life story from your emails — powered by Gemini and function-calling agents.

This project demonstrates a complete agent ecosystem built using:
Agent routing
Multi-step agent workflows
Strict function-calling
Gmail automation tools

## Problem
Everyone has a dead Gmail with 100k+ unread emails.

## Magic Solution
Say "Clean my inbox" → agent fetches emails and trashes/archives it, unsubscribes.

## Demo Video (2:48)


## 10 Required Features Used
1. Multi-agent system (5 agents)
2. Parallel + Sequential + Loop agents
3. Gemini 2.5 Flash powered
4. Tools (Gmail API OpenAPI + custom PDF)
5. Long-running pause/resume (45s unsubscribe)
6. Sessions & Memory + Vector MemoryBank (life events)
7. Observability (OpenTelemetry traces)
8. A2A Protocol (agents debate archiving mom’s emails)
9. Agent deployment ready (Docker)
10. Real Gmail OAuth

Run: mvn package → java -jar target/gmail-life-support-1.0.jar → type "rescue my gmail"

Built 100% with Google Java ADK + Gemini + Gmail API
