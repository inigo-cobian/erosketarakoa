package com.erosketarakoa.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sqrt

class RoundedHexagonShape(
    private val cornerRadius: Dp = 8.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Pointy-top regular hexagon: circumradius r satisfies
        //   width  = r * sqrt(3)
        //   height = 2r
        // Pick the largest r that fits inside the bounding box.
        val r = min(size.width / sqrt(3f), size.height / 2f)

        // Clamp so the two cut-points on each edge don't cross.
        // For a regular hexagon, edge length == r, so cornerRadius must be < r/2.
        val cr = min(with(density) { cornerRadius.toPx() }, r * 0.45f)

        val halfSqrt3 = sqrt(3f) / 2f
        val vertices = arrayOf(
            Offset(cx, cy - r),                             // top
            Offset(cx + r * halfSqrt3, cy - r / 2f),        // upper-right
            Offset(cx + r * halfSqrt3, cy + r / 2f),        // lower-right
            Offset(cx, cy + r),                             // bottom
            Offset(cx - r * halfSqrt3, cy + r / 2f),        // lower-left
            Offset(cx - r * halfSqrt3, cy - r / 2f)         // upper-left
        )
        val n = vertices.size

        // For each vertex i:
        //   starts[i] = point on edge (i-1 -> i), cr away from vertex i
        //   ends[i]   = point on edge (i -> i+1), cr away from vertex i
        val starts = Array(n) { Offset.Zero }
        val ends   = Array(n) { Offset.Zero }
        for (i in 0 until n) {
            val curr = vertices[i]
            val prev = vertices[(i - 1 + n) % n]
            val next = vertices[(i + 1) % n]
            starts[i] = curr + (prev - curr).normalized() * cr
            ends[i]   = curr + (next - curr).normalized() * cr
        }

        val path = Path().apply {
            moveTo(ends[0].x, ends[0].y)
            for (i in 1 until n) {
                lineTo(starts[i].x, starts[i].y)
                quadraticBezierTo(
                    vertices[i].x, vertices[i].y,
                    ends[i].x, ends[i].y
                )
            }
            // Wrap around and close through vertex 0
            lineTo(starts[0].x, starts[0].y)
            quadraticBezierTo(
                vertices[0].x, vertices[0].y,
                ends[0].x, ends[0].y
            )
            close()
        }

        return Outline.Generic(path)
    }
}

private fun Offset.normalized(): Offset {
    val d = getDistance()
    return if (d == 0f) Offset.Zero else this / d
}
