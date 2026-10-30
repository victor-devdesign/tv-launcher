#!/usr/bin/env bash
# Restores the system apps removed for user 0 (third-party apps must be reinstalled by hand).
cd "$(dirname "$0")"
./adb shell cmd package install-existing --user 0 com.google.android.katniss
./adb shell cmd package install-existing --user 0 com.google.android.play.games
./adb shell cmd package install-existing --user 0 com.google.android.videos
./adb shell cmd package install-existing --user 0 com.google.android.youtube.tvmusic
./adb shell cmd package install-existing --user 0 com.google.android.tv
./adb shell cmd package install-existing --user 0 com.google.android.tvlauncher
./adb shell cmd package install-existing --user 0 com.google.android.tvrecommendations
./adb shell cmd package install-existing --user 0 com.google.android.backdrop
./adb shell cmd package install-existing --user 0 com.google.android.tv.bugreportsender
./adb shell cmd package install-existing --user 0 com.google.android.feedback
./adb shell cmd package install-existing --user 0 com.google.android.tv.remote.service
./adb shell cmd package install-existing --user 0 com.google.android.speech.pumpkin
./adb shell cmd package install-existing --user 0 com.google.android.syncadapters.calendar
./adb shell cmd package install-existing --user 0 com.google.android.syncadapters.contacts
./adb shell cmd package install-existing --user 0 com.google.android.apps.mediashell
./adb shell cmd package install-existing --user 0 com.mitv.tvhome.atv
./adb shell cmd package install-existing --user 0 com.mitv.tvhome.michannel
./adb shell cmd package install-existing --user 0 com.mitv.dream
./adb shell cmd package install-existing --user 0 com.mitv.videoplayer
./adb shell cmd package install-existing --user 0 com.miui.tv.analytics
./adb shell cmd package install-existing --user 0 tv.alphonso.alphonso_eula
