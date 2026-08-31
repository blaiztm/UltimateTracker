# UltimateTracker

UltimateTracker is my small pet project designed for myself. It's a personal movie, TV series, and anime tracker built with Kotlin and Jetpack Compose. And you are free to use it!

## Features

- Four watch statuses with quick status changes directly from collection cards.
- Built-in and custom media types.
- Genre and keyword tags with combined filtering.
- Local persistence with Room.
- English and Russian in-app language selection; English is the default.
- TMDB-powered catalog search, title suggestions, and automatic cover lookup.
- System Photo Picker for choosing a local cover.
- Portable JSON backup import/export for moving all lists, titles, metadata, and local covers between devices.

## TMDB setup

You can use your own TMDB (movies database) API read token from settings.

## App setup

1. Go into realeses and download apk file.
2. Install apk on your android phone

## Architecture

The project has one `app` module and uses a lightweight MVVM architecture. Compose screens send events to a ViewModel, the ViewModel exposes `StateFlow`, the repository coordinates Room, and Room stores the collection in a local SQLite database. Online search is isolated in a small TMDB client and does not affect offline storage.
