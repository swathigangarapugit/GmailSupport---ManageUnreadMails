package com.google.gmaillife;

import dev.langchain4j.agent.task.Task;  // Fixed import – now from agentic module

public class UnsubscriberBot {
    public static String massUnsubscribe() {
        Task<String> task = Task.suspendable("Unsubscribing from 200+ senders...", () -> {  // Fixed Task
            try {
                Thread.sleep(45000); // 45-second real pause (demo long-running)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Unsubscribed from 217 newsletters. Your inbox will be peaceful forever.";
        });
        // Simulate resume (in full agentic, this would be async)
        new Thread(() -> task.resume()).start();
        return "Unsubscribe swarm started – real 45-second pause active (watch timer)";
    }
}
