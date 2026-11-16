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

    // The components' words.
    val setting = t("Setting", "Настройка")
    val menuItems =
        listOf(t("TRACKING", "ТРЕКИНГ"), t("CHROMA", "ЦВЕТНОСТЬ"), t("SHARPNESS", "ЧЁТКОСТЬ"), t("RESET", "СБРОС"))
    val monthNames =
        if (language == SampleLanguage.English) {
            listOf(
                "JANUARY",
                "FEBRUARY",
                "MARCH",
                "APRIL",
                "MAY",
                "JUNE",
                "JULY",
                "AUGUST",
                "SEPTEMBER",
                "OCTOBER",
                "NOVEMBER",
                "DECEMBER",
            )
        } else {
            listOf(
                "ЯНВАРЬ",
                "ФЕВРАЛЬ",
                "МАРТ",
                "АПРЕЛЬ",
                "МАЙ",
                "ИЮНЬ",
                "ИЮЛЬ",
                "АВГУСТ",
                "СЕНТЯБРЬ",
                "ОКТЯБРЬ",
                "НОЯБРЬ",
                "ДЕКАБРЬ",
            )
        }
    val weekdayNames =
        if (language ==
            SampleLanguage.English
        ) {
            listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
        } else {
            listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
        }
    val columnRecord = t("RECORD", "ЗАПИСЬ")
    val columnLength = t("LENGTH", "ДЛИНА")
    val columnChannel = t("CHANNEL", "КАНАЛ")
    val noSignal = t("No signal", "Нет сигнала")
    val night = t("night", "ночь")
    val nightToTuesday = t("Tuesday night", "ночь на вторник")
    val nightAir = t("night air", "ночной эфир")
    val clear = t("Clear", "Очистить")
    val openDrawer = t("Channels", "Каналы")
    val channels = t("CHANNELS", "КАНАЛЫ")
    val channelNames =
        listOf(t("On air", "Эфир"), t("Queue", "Очередь"), t("Search", "Поиск"), t("Settings", "Настройка"))
    val refreshingWord = t("REFRESHING", "ОБНОВЛЯЕМ")
    val refreshedWord = t("REFRESHED", "ОБНОВЛЕНО")
    val openSheet = t("Recording speed", "Скорость записи")
    val done = t("Done", "Готово")
    val share = t("SHARE", "ПОДЕЛИТЬСЯ")
    val recLeft = t("3 MIN LEFT", "ОСТАЛОСЬ 3 МИН")

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

    // The Components page's structure (B-99).
    val rowVariants = t("VARIANTS", "ВАРИАНТЫ")
    val rowStates = t("STATES", "СОСТОЯНИЯ")
    val rowTry = t("TRY IT", "ПОПРОБУЙТЕ")
    val groupMvp = t("Components", "Компоненты")
    val groupMvpLine =
        t(
            "The 21 components of the first release, with every state the design draws.",
            "21 компонент первого выпуска со всеми состояниями, которые рисует дизайн.",
        )
    val groupV1 = t("Components v1", "Компоненты v1")
    val groupV1Line =
        t(
            "The second release: slider, segments, tooltip, menu, drawer, sheet and pull to refresh.",
            "Второй выпуск: слайдер, сегменты, подсказка, меню, боковое меню, лист и «потянуть».",
        )
    val groupLater = t("Components later", "Компоненты позже")
    val groupLaterLine =
        t("Search, date and time, the table and the carousel.", "Поиск, дата и время, таблица и карусель.")
    val groupBase = t("Foundations", "Основа")
    val groupBaseLine =
        t(
            "Icons, patterns, the Tape and the keyframe loss every screen stands on.",
            "Иконки, узоры, Лента и потеря кадра, на которых стоит каждый экран.",
        )
    val stageMvp = "MVP"
    val stageV1 = "V1"
    val stageLater = t("LATER", "ПОЗЖЕ")
    val stageBase = t("BASE", "ОСНОВА")
    val onThisPage = t("ON THIS PAGE", "НА СТРАНИЦЕ")

    // State captions, as the design's table names them.
    val stRest = t("rest", "покой")
    val stPressed = t("pressed", "нажатие")
    val stLoading = t("loading", "загрузка")
    val stSuccess = t("success", "готово")
    val stDisabled = t("disabled", "выключено")
    val stSelected = t("selected", "выбрано")
    val stUnselected = t("unselected", "не выбрано")
    val stChecked = t("checked", "отмечено")
    val stUnchecked = t("unchecked", "не отмечено")
    val stOn = t("on", "вкл.")
    val stOff = t("off", "выкл.")
    val stFocused = t("focused", "фокус")
    val stError = t("error", "ошибка")
    val stDragging = t("dragging", "перетаскивание")
    val stOpen = t("open", "открыто")
    val stShown = t("shown", "показана")
    val stRefreshing = t("refreshing", "обновление")
    val stDeterminate = t("determinate", "определённый")
    val stIndeterminate = t("indeterminate", "неопределённый")
    val stRowSelected = t("row selected", "строка выбрана")

    // Variant captions.
    val vFilled = t("filled", "заливка")
    val vOutlined = t("outlined", "контур")
    val vText = t("text", "текст")
    val vFab = t("FAB", "FAB")
    val vExtended = t("extended", "расширенная")
    val vBlock = t("a block", "блок")
    val vRow = t("a row", "строка")
    val vTrailingText = t("trailing text", "текст в хвосте")
    val vTrailingBadge = t("trailing badge", "бейдж в хвосте")
    val vPlain = t("plain", "простая")
    val vSupporting = t("with a second line", "со второй строкой")
    val vSuggestions = t("suggestions", "подсказки")
    val vResults = t("results, one loading", "результаты, один грузится")
    val vLoud = t("loud", "громкий")
    val vQuiet = t("quiet", "тихий")
    val hover = t("hover, focus or hold", "наведите, сфокусируйте или удержите")
    val pullHint = t("pull the list down", "потяните список вниз")
    val previousMonth = t("Previous month", "Предыдущий месяц")
    val nextMonth = t("Next month", "Следующий месяц")
    val hours = t("Hours", "Часы")
    val minutes = t("Minutes", "Минуты")

    // The demo app's own words (B-96), designed here.
    val menu = t("Menu", "Меню")
    val back = t("Back", "Назад")
    val rename = t("Rename", "Переименовать")
    val shareWord = t("Share", "Поделиться")
    val newRecording = t("New recording", "Новая запись")
    val pause = t("Pause", "Пауза")
    val volume = t("VOLUME", "ГРОМКОСТЬ")
    val recentlyOnAir = t("RECENTLY ON AIR", "НЕДАВНО В ЭФИРЕ")
    val onTheShelf = t("ON THE SHELF", "НА ПОЛКЕ")
    val rain = t("Rain over the transmitter", "Дождь над передатчиком")
    val sideB = t("Side B · 31 min", "Сторона B · 31 мин")
    val staticBetween = t("Static between channels", "Помехи между каналами")
    val noiseNine = t("Noise · 9 min", "Шум · 9 мин")
    val weather = t("Weather at three a.m.", "Погода в три часа ночи")
    val recordedOverTwenty = t("Recorded over · 20 min", "Записано поверх · 20 мин")
    val lateRepeat = t("Late-night repeat", "Ночной повтор")
    val freshOffTheAir = t("Fresh off the air", "Только что из эфира")
    val justNow = t("Just now · 3 min", "Только что · 3 мин")
    val copying = t("COPYING · 2 OF 3", "КОПИРУЕМ · 2 ИЗ 3")
    val pullToRefresh = t("PULL DOWN FOR NEW TAPES", "ПОТЯНИТЕ ВНИЗ ЗА НОВЫМИ")

    fun inLine(n: Int) = t("$n IN LINE", "$n В ОЧЕРЕДИ")

    val rainWord = t("rain", "дождь")
    val noiseWord = t("static", "помехи")
    val archiveWord = t("ARCHIVE", "АРХИВ")
    val sortOrders = listOf(t("AS FILED", "КАК НА ПОЛКЕ"), t("LONGEST", "САМЫЕ ДЛИННЫЕ"), t("BY CHANNEL", "ПО КАНАЛУ"))

    fun sortBy(order: Int) = sortOrders[order].lowercase().replaceFirstChar { it.uppercase() }

    fun eraseSelected(n: Int) =
        if (n ==
            0
        ) {
            t("Select to erase", "Выберите, что стереть")
        } else {
            t("Erase $n", "Стереть $n")
        }

    val notesText =
        t(
            "Taped off the night air. The tracking drifts after minute 30, and the head noise is the tape's own.",
            "Записано с ночного эфира. После 30-й минуты плывёт трекинг, а шум головки у плёнки свой.",
        )
    val master = t("Master", "Мастер")
    val secondGeneration = t("2nd-generation copy", "Копия 2-го поколения")
    val copyToBlank = t("Copy to a blank tape", "Копировать на чистую кассету")
    val sendACopy = t("Send a copy", "Отправить копию")
    val printLabel = t("Print the label", "Напечатать этикетку")
    val picture = t("Picture", "Картинка")
    val pictureModes = listOf(t("STANDARD", "ОБЫЧНАЯ"), t("SOFT", "МЯГКАЯ"), t("SHARP", "РЕЗКАЯ"))
    val sleepTimer = t("SLEEP TIMER", "ТАЙМЕР СНА")
    val sleepOptions = listOf(t("OFF", "ВЫКЛ"), t("30 MIN", "30 МИН"), t("60 MIN", "60 МИН"))
    val timerRecording = t("Timer recording", "Запись по таймеру")

    fun timerLine(
        day: Int,
        month: Int,
        hour: Int,
        minute: Int,
    ): String {
        val two = { v: Int -> v.toString().padStart(2, '0') }
        return "${two(day)}.${two(month)} · $hour:${two(minute)}"
    }
}
