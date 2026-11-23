package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantPullToRefresh
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.coroutines.delay

@Composable
internal fun Queue(
    s: SampleStrings,
    queue: MutableList<Cassette>,
    nav: DemoNav,
    tapes: List<Cassette>,
    bar: @Composable BoxScope.() -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    var copied by remember { mutableFloatStateOf(.42f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            copied = if (copied >= 1f) 0f else (copied + .01f).coerceAtMost(1f)
        }
    }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(2000)
            if (queue.none { it.title == s.freshOffTheAir }) {
                queue.add(0, Cassette(s.freshOffTheAir, s.justNow, "03:15", "CH 04", 1, false))
            }
            refreshing = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            TabBar(s.queue, s, nav, status = s.inLine(queue.size))
            Spacer(Modifier.height(8.dp))
            Osd(s.copying, size = 18)
            Spacer(Modifier.height(10.dp))
            MutantLinearProgress(copied)
            Spacer(Modifier.height(18.dp))
            MutantPullToRefresh(
                refreshing,
                { refreshing = true },
                Modifier.fillMaxWidth().weight(1f),
                refreshingLabel = s.refreshingWord,
                doneLabel = s.refreshedWord,
                key = "demo-ptr",
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // B-174: a row and its divider are keyed by the record, so a tape put in at the top moves the rows
                    // below it with their tiles, dividers and remembered state instead of handing each the one above.
                    for (t in queue) {
                        key(t.title) {
                            val fresh = t.title == s.freshOffTheAir
                            MutantListItem(
                                t.title,
                                supporting = t.side,
                                leadingIcon = MutantIcons.Schedule,
                                trailingText = if (fresh) null else t.length,
                                trailing = if (fresh) ({ MutantBadge(s.brandNew) }) else null,
                                onClick = { tapes.indexOf(t).takeIf { it >= 0 }?.let { nav.open = it } },
                                key = "queue-${t.title}",
                            )
                            MutantDivider(key = "q-${t.title}")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Osd(s.pullToRefresh, MutantTheme.colors.textSecondary, size = 16)
                    Spacer(Modifier.height(112.dp))
                }
            }
        }
        bar()
    }
}
