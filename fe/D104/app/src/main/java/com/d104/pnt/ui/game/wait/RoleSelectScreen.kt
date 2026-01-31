package com.d104.pnt.ui.game.wait

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.DarkBackground
import timber.log.Timber

@Composable
fun RoleSelectScreen(
    onRoleSelected: (GameRole) -> Unit,
    onBackPressed: () -> Boolean
) {
    Surface(modifier = Modifier.fillMaxSize()) {

        // 배경 이미지
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.img_game_bg_1),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "선호 진영을 선택해주세요",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                color = Color.White
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleCard(
                    role = GameRole.POLICE,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected(GameRole.POLICE) }
                )
                RoleCard(
                    role = GameRole.THIEF,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected(GameRole.THIEF) }
                )
            }

            Spacer(Modifier.height(20.dp))

            PixelContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onRoleSelected(GameRole.ANY)
                    },
                backgroundColor = DarkBackground,
                borderColor = Color.White,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "상관없음",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RoleCard(role: GameRole, modifier: Modifier, onClick: () -> Unit) {
    PixelContainer(
        modifier = modifier.clickable(onClick = {
            onClick()
            Timber.d("clicked!")
        }),
        backgroundColor = DarkBackground,
        borderColor = role.color
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(role.emoji),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Text(
                text = role.roleName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                color = Color.White
            )

            Text(
                text = "영차 케로챠 영차 케로챠 영차",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                color = Color.White
            )

        }


    }
}