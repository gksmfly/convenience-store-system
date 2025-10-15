@file:Suppress("PackageDirectoryMismatch")
package store

import java.time.LocalDate

/** 테스트 용이성을 위해 날짜를 주입하는 시계 인터페이스 */
interface Clock { fun today(): LocalDate }

/** 실제 실행용 시스템 시계 */
object SystemClock : Clock {
    override fun today(): LocalDate = LocalDate.now()
}