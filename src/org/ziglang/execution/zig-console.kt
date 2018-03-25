package org.ziglang.execution

import com.intellij.execution.filters.ConsoleFilterProviderEx
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.filters.UrlFilter
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.regex.Pattern

class ZigConsoleFilter(private val project: Project) : Filter {
	companion object {
		//后面那两个字符是保险用的。。。
		private val ERROR_FILE_LOCATION = Pattern.compile("(.+\\.zig):([0-9]+):([0-9]+): ")
	}

	override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
		val startIndex = entireLength - line.length
		if (startIndex != 0) {
			val matcher = ERROR_FILE_LOCATION.matcher(line)

			if (matcher.lookingAt()) {
				val resultFile = project.baseDir.fileSystem.findFileByPath(matcher.group(1)) ?: return null
				val lineNumber = matcher.group(2).toIntOrNull() ?: return null
				val columnNumber = matcher.group(3).toIntOrNull() ?: return null

				return Filter.Result(
					startIndex,
					startIndex + matcher.end() - 1,    //把冒号也扔进去吧emmmmm
					OpenFileHyperlinkInfo(
						project,
						resultFile,
						lineNumber.let { if (it > 0) it - 1 else it },
						columnNumber
					)
				)
			}
		}

		return null
	}
}

class ZigConsoleFilterProvider : ConsoleFilterProviderEx {
	override fun getDefaultFilters(project: Project, scope: GlobalSearchScope) = getDefaultFilters(project)
	override fun getDefaultFilters(project: Project) = arrayOf(ZigConsoleFilter(project), UrlFilter())
}