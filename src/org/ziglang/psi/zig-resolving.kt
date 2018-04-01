package org.ziglang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import org.ziglang.ZigTokenType
import org.ziglang.orFalse
import org.ziglang.psi.impl.ZigAbstractSymbol

/**
 * 今天就要成为吊打冰酱的人（超大雾
 */
class ZigSymbolRef(
		private val symbol: ZigAbstractSymbol,
		private var refTo: PsiElement? = null
) : PsiPolyVariantReference {
	private val range = TextRange(0, element.textLength)
	private val isDeclaration = (element as? ZigSymbol)?.isDeclaration.orFalse()
	private val resolver by lazy {
		if (element is ZigMacroExpr) macroResolver
		else symbolResolver
	}

	override fun equals(other: Any?) = (other as? ZigSymbolRef)?.element == element
	override fun getElement(): PsiElement = symbol
	override fun hashCode() = element.hashCode()
	override fun getRangeInElement() = range
	override fun isSoft() = true
	override fun resolve() = if (isDeclaration) null else multiResolve(false).firstOrNull()?.element
	override fun isReferenceTo(o: PsiElement?) = o === refTo || o === resolve()
	override fun getVariants(): Array<LookupElementBuilder> {
		val variantsProcessor = CompletionProcessor(this, true)
		treeWalkUp(variantsProcessor, element, element.containingFile)
		return variantsProcessor.candidateSet.toTypedArray()
	}

	override fun getCanonicalText(): String = element.text
	override fun handleElementRename(newName: String) = ZigTokenType.fromText(newName, element.project).let(element::replace)
	override fun bindToElement(element: PsiElement) = element.also { refTo = element }
	override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
		val file = element.containingFile ?: return emptyArray()
		if (isDeclaration or !element.isValid or element.project.isDisposed) return emptyArray()
		return ResolveCache.getInstance(element.project)
				.resolveWithCaching(this, resolver, true, incompleteCode, file)
	}

	private companion object ResolverHolder {
		private val symbolResolver = ResolveCache.PolyVariantResolver<ZigSymbolRef> { ref, incompleteCode ->
			resolveWith(SymbolResolveProcessor(ref, incompleteCode), ref)
		}

		private val macroResolver = ResolveCache.PolyVariantResolver<ZigSymbolRef> { ref, incompleteCode ->
			resolveWith(MacroSymbolResolveProcessor(ref, incompleteCode), ref)
		}

		private inline fun <reified T> resolveWith(processor: ResolveProcessor<T>, ref: ZigSymbolRef): Array<T> {
			val file = ref.element.containingFile ?: return emptyArray()
			treeWalkUp(processor, ref.element, file)
			return processor.candidateSet.toTypedArray()
		}
	}
}

abstract class ResolveProcessor<ResolveResult>(private val place: PsiElement) : PsiScopeProcessor {
	abstract val candidateSet: ArrayList<ResolveResult>
	override fun handleEvent(event: PsiScopeProcessor.Event, o: Any?) = Unit
	override fun <T : Any?> getHint(hintKey: Key<T>): T? = null
	protected val PsiElement.hasNoError get() = (this as? StubBasedPsiElement<*>)?.stub != null || !PsiTreeUtil.hasErrorElements(this)
	fun isInScope(element: PsiElement) = when {
	//TODO impl
		else -> false
	}
}

open class SymbolResolveProcessor(
		@JvmField protected val name: String,
		place: PsiElement,
		private val incompleteCode: Boolean) :
		ResolveProcessor<PsiElementResolveResult>(place) {
	constructor(ref: ZigSymbolRef, incompleteCode: Boolean) : this(ref.canonicalText, ref.element, incompleteCode)

	override val candidateSet = ArrayList<PsiElementResolveResult>(3)
	protected open fun accessible(element: PsiElement) = name == element.text && isInScope(element)
	override fun execute(element: PsiElement, resolveState: ResolveState) = when {
		candidateSet.isNotEmpty() -> false
		element is ZigSymbol -> {
			val accessible = accessible(element) and element.isDeclaration
			if (accessible) candidateSet += PsiElementResolveResult(element, element.hasNoError)
			!accessible
		}
		else -> true
	}
}

class MacroSymbolResolveProcessor(name: String, place: PsiElement, incompleteCode: Boolean) :
		SymbolResolveProcessor(name, place, incompleteCode) {
	constructor(ref: ZigSymbolRef, incompleteCode: Boolean) : this(ref.canonicalText, ref.element, incompleteCode)

	override fun accessible(element: PsiElement) = "@${element.text}" == name && isInScope(element)
}

class CompletionProcessor(place: PsiElement, private val incompleteCode: Boolean) :
		ResolveProcessor<LookupElementBuilder>(place) {
	constructor(ref: ZigSymbolRef, incompleteCode: Boolean) : this(ref.element, incompleteCode)

	override val candidateSet = ArrayList<LookupElementBuilder>(20)
	override fun execute(element: PsiElement, resolveState: ResolveState): Boolean {
//		if (element is JuliaSymbol) {
//			val (icon, value, tail, type) = when {
//				TODO impl QAQ
//				else -> return true
//			}
//			if (element.isDeclaration and element.hasNoError and isInScope(element)) candidateSet += LookupElementBuilder
//					.create(value)
//					.withIcon(icon)
//					// tail text, it will not be completed by Enter Key press
//					.withTailText(tail, true)
//					// the type of return value,show at right of popup
//					.withTypeText(type, true)
//		}
		return true
	}
}