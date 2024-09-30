package ua.acclorite.book_story.domain.repository

interface BookRepository :
    BookManagementRepository,
    DataStoreManagementRepository,
    FileSystemManagementRepository,
    HistoryManagementRepository,
    ApiManagementRepository,
    ColorPresetManagementRepository,
    FavoriteDirectoryManagementRepository