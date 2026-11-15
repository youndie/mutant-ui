package io.github.youndie.mutant.sample

/** The sample's two languages (research D11): it opens in English, the owner's choice; Russian is the design's own. */
enum class SampleLanguage { English, Russian }

/**
 * Every word the sample shows, in both languages. The Russian ones are the design's demo strings, verbatim; the English
 * ones say the same. The brief asks for the demo to be checked in Cyrillic and in Latin.
 */
class SampleStrings(
    val language: SampleLanguage,
) {
    private fun t(
        en: String,
        ru: String,
    ) = if (language == SampleLanguage.English) en else ru

    // The header.
    val kit = "mutant-ui"
    val themeEfir = t("On air", "Эфир")
    val themeKopiya = t("Copy", "Копия")
    val reduceMotion = t("Reduce motion", "Без движения")
    val cleanSignal = t("Clean signal", "Чистый сигнал")
    val languageName = t("EN", "RU")
    val pageComponents = t("Components", "Компоненты")
    val pageDemo = t("Demo", "Демо")

    // Section titles, as the pages group the components.
    val sectionButtons = t("Buttons", "Кнопки")
    val sectionIconButtons = t("Icon buttons · FAB", "Кнопки-иконки · FAB")
    val sectionChips = t("Chips", "Чипы")
    val sectionSelection = t("Checkbox · Radio · Switch", "Чекбокс · Радио · Переключатель")
    val sectionSlider = t("Slider", "Слайдер")
    val sectionSearch = t("Search bar", "Строка поиска")
    val night = t("night", "ночь")
    val nightToTuesday = t("Tuesday night", "ночь на вторник")
    val nightAir = t("night air", "ночной эфир")
    val clear = t("Clear", "Очистить")
    val sectionSegmented = t("Segmented button", "Сегменты")
    val sectionTooltip = t("Tooltip", "Подсказка")
    val sectionSheet = t("Bottom sheet", "Нижний лист")
    val sectionPull = t("Pull to refresh", "Потянуть, чтобы обновить")
    val refreshingWord = t("REFRESHING", "ОБНОВЛЯЕМ")
    val refreshedWord = t("REFRESHED", "ОБНОВЛЕНО")
    val openSheet = t("Recording speed", "Скорость записи")
    val done = t("Done", "Готово")
    val share = t("SHARE", "ПОДЕЛИТЬСЯ")
    val recLeft = t("3 MIN LEFT", "ОСТАЛОСЬ 3 МИН")
    val sectionField = t("Text field", "Поле ввода")
    val sectionCard = t("Card · Cocoon", "Карточка · Кокон")
    val sectionNav = t("Top app bar · Tabs · Navigation bar", "Верхняя панель · Вкладки · Навигация")
    val sectionList = t("List · Divider · Badge", "Список · Разделитель · Бейдж")
    val sectionSnackbar = t("Snackbar", "Строка-уведомление")
    val sectionProgress = t("Progress", "Прогресс")
    val sectionDialog = t("Dialog", "Диалог")
    val sectionEmpty = t("Empty states", "Пустые состояния")
    val sectionIcons = t("Icons · Patterns · Tape", "Иконки · Узоры · Лента")

    // The components' words.
    val play = t("Play", "Воспроизвести")
    val playAll = t("Play all", "Воспроизвести всё")
    val reset = t("Reset", "Сбросить")
    val record = t("Record", "Записать")
    val loading = t("Loading", "Загрузка")
    val disabled = t("Disabled", "Disabled")
    val favourites = t("Favourites", "Избранное")
    val all = t("All", "Все")
    val recordings = t("Recordings", "Записи")
    val noise = t("Noise", "Шум")
    val headNoise = t("Head noise", "Шум головки")
    val autoTracking = t("Auto tracking", "Автотрекинг")
    val tracking = t("TRACKING", "ТРЕКИНГ")
    val sharpness = t("SHARPNESS", "ЧЁТКОСТЬ")
    val longPlay = t("LP · 4 hours", "LP · 4 часа")
    val standardPlay = t("SP · 2 hours", "SP · 2 часа")
    val clean = t("Clean signal", "Чистый сигнал")
    val frameLoss = t("Frame loss", "Потеря кадра")
    val search = t("SEARCH", "ПОИСК")
    val findRecording = t("Find a recording", "Найти запись")
    val untitled = t("Untitled No. 3", "Без подписи №3")
    val sideA = t("Side A · 46 min", "Сторона A · 46 мин")
    val nightTuesday = t("On air, Tuesday night", "Эфир, ночь на вторник")
    val recordedOver = t("Recorded over · 12 min", "Записано поверх · 12 мин")
    val onAir = t("On air", "Эфир")
    val status = "CH 03 · SP"
    val tracks = t("Tracks", "Дорожки")
    val notes = t("Notes", "Заметки")
    val copies = t("Copies", "Копии")
    val queue = t("Queue", "Очередь")
    val find = t("Search", "Поиск")
    val settings = t("Settings", "Настройка")
    val testCard = t("Test card", "Настроечная таблица")
    val fourthGeneration = t("4th-generation copy", "Копия 4-го поколения")
    val brandNew = t("NEW", "НОВОЕ")
    val queued = t("Added to the queue", "Добавлено в очередь")
    val undo = t("UNDO", "ОТМЕНИТЬ")
    val showSnackbar = t("Show the snackbar", "Показать уведомление")
    val eraseQuestion = t("Erase the recording?", "Стереть запись?")
    val eraseText = t("The tape can be recorded again.", "Плёнку можно будет записать заново.")
    val cancel = t("Cancel", "Отмена")
    val erase = t("Erase", "Стереть")
    val openDialog = t("Open the dialog", "Открыть диалог")
    val emptyTitle = t("Nothing here yet", "Здесь пока пусто")
    val emptyText =
        t("Record your first cassette, and it will be on air.", "Запишите первую кассету, и она появится в эфире.")
    val tapeOver = t("The Tape over a card", "Лента поверх карточки")
    val demoLibrary = t("Library", "Эфир")
    val demoRecording = t("Recording", "Запись")
    val demoSettings = t("Settings under a dialog", "Настройка под диалогом")
    val clock = "CH 03 · SP · 0:42:17"
    val playAllWide = t("Play all", "Воспроизвести всё")
    val sideAShort = t("Side A", "Сторона A")
    val sideALine = t("46 MIN · SP · 4TH-GENERATION COPY", "46 МИН · SP · КОПИЯ 4-ГО ПОКОЛЕНИЯ")
    val toQueue = t("Queue", "В очередь")
    val recordedOverShort = t("Recorded over", "Записано поверх")
    val name = t("NAME", "ИМЯ")
    val nightTue = t("Night on Tue", "Ночь на вт")
    val channel = t("CHANNEL", "КАНАЛ")
    val noSignal99 = t("NO SIGNAL ON CHANNEL 99", "НЕТ СИГНАЛА НА КАНАЛЕ 99")
    val speed = t("RECORDING SPEED", "СКОРОСТЬ ЗАПИСИ")
    val keep = t("Keep", "Не надо")
    val listen = t("Listen", "Слушать")
    val eraseNamed =
        t(
            "“On air, Tuesday night” will be erased. The tape can be recorded again.",
            "«Эфир, ночь на вторник» будет стёрта. Плёнку можно будет записать заново.",
        )
    val resetSettings = t("Reset settings", "Сбросить настройки")
}
