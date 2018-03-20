package org.ziglang.action

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiDirectory
import icons.ZigIcons
import org.ziglang.ZigBundle
import org.ziglang.editing.ZigNameValidator
import org.ziglang.project.zigSettings


class NewZigFile : CreateFileFromTemplateAction(
	ZigBundle.message("zig.actions.new-file.title"),
	ZigBundle.message("zig.actions.new-file.description"),
	ZigIcons.ZIG_BIG_ICON
), DumbAware {
	companion object {
		fun createProperties(project: Project, className: String) =
			FileTemplateManager.getInstance(project).defaultProperties.also { properties ->
				properties += "ZIG_VERSION" to project.zigSettings.settings.version
				properties += "NAME" to className
			}
	}

	override fun getActionName(dir: PsiDirectory?, name: String?, templateName: String?) =
		ZigBundle.message("zig.actions.new-file.title")

	override fun buildDialog(project: Project?, dir: PsiDirectory?, builder: CreateFileFromTemplateDialog.Builder) {
		builder
			.setTitle(ZigBundle.message("zig.actions.new-file.title"))
			.setValidator(ZigNameValidator)
			.addKind("File", ZigIcons.ZIG_FILE, "Zig File")
		//.addKind("Other", ZigIcons.ZIG_BIG_ICON, "Zig Other")		For test
	}

	override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) =
		try {
			val className = FileUtilRt.getNameWithoutExtension(name)
			val project = dir.project
			val properties = createProperties(project, className)
			CreateFromTemplateDialog(project, dir, template, AttributesDefaults(className).withFixedName(true), properties)
				.create()
				.containingFile
		} catch (e: Exception) {
			LOG.error("Error while creating new file", e)
			null
		}


}