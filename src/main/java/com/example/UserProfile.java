package com.example;

record UserProfile(
        String handle,
        String rank,
        String maxRank,
        int rating,
        int maxRating,
        String country,
        String organization,
        long registrationTimeSeconds,
        long lastOnlineTimeSeconds,
        String avatarUrl,
        int problemsSolved,
        int currentStreak,
        int longestStreak,
        int totalSubmissions
) {}
