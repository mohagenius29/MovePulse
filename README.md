# MovePulse

**MovePulse** is a healthy exercise Android application designed to make fitness accessible, motivating, and smart — right from your home.

## Overview

MovePulse helps you stay active with the best home-based activities. It goes beyond simple tracking by detecting real-world movements such as walking, running, jogging, dancing, and squatting. The app provides encouraging notifications, calorie estimates, and even basic heart rate readings using your phone.

## Key Features

### Activity Detection
- **Walking, Running & Jogging**: Automatically detects and counts steps/movements using device sensors.
- **Dancing & Squatting**: Motion recognition for more dynamic home exercises.
- **No Motion Detection**: Smart alerts when inactivity is detected for too long.

### Motivation & Notifications
- Real-time encouragement based on your progress.
- Example: *"You walked 500 steps — that's about 40 calories burned! Keep going, you're doing amazing! 💪"*
- Inventive, positive, and impressive motivational messages tailored to your activity.
- Gentle reminders when no motion is detected to help you stay consistent.

### Route Map
- Walks, jogs and runs are traced with GPS and drawn on an OpenStreetMap map (no API key needed), with the day's distance.

### Daily Analysis
- Steps per hour today and per day over the last 7 days, calories, active minutes and an activity mix (walking, jogging, running, other movement).
- Plain-language insights: goal progress, comparison with yesterday, most active hour, long inactive stretches, best day of the week.

### Heart Rate Monitoring (planned)
- Fingertip-over-camera method (photoplethysmography), because a phone resting on the chest cannot read BPM reliably.

### Home Exercise Focus
- Curated best home activities that require little to no equipment.
- Designed for everyday users who want to stay fit without leaving the house.

## Vision

MovePulse aims to turn everyday movement into a rewarding habit. Whether you're walking around the house, dancing to your favorite song, or doing a quick set of squats, the app notices, counts it, motivates you, and helps you track progress in a fun and supportive way.

## Status

First build (v0.1): step counting, activity classification (still / moving / walking / jogging / running), calorie estimates, milestone and inactivity notifications, route map and daily analysis. Squat/dance rep detection, the home workout library and heart rate are next.

## Build

1. Open the project in Android Studio (Koala or newer). It creates the Gradle wrapper on first sync.
2. Run on a real phone (emulators have no step sensor): **Build > Build APK(s)**.
3. Grant Physical activity, Location and Notifications permissions when asked.

Note: calories use MET values and your weight (default 70 kg, change it on the Today screen), so 500 steps is roughly 20-30 kcal.

---

**Stay moving. Stay motivated. MovePulse.**
