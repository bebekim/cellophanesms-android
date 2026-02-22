package com.cellophanemail.sms.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.domain.annotation.AnnotationPipeline
import com.cellophanemail.sms.domain.model.TextAnnotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DemoViewModel @Inject constructor(
    private val annotationPipeline: AnnotationPipeline
) : ViewModel() {

    private val _textState = MutableStateFlow(SAMPLE_SMS)
    val textState: StateFlow<String> = _textState.asStateFlow()

    private val _annotationsState = MutableStateFlow<List<TextAnnotation>>(emptyList())
    val annotationsState: StateFlow<List<TextAnnotation>> = _annotationsState.asStateFlow()

    init {
        observeTextChanges()
    }

    fun onTextChanged(text: String) {
        _textState.value = text
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeTextChanges() {
        viewModelScope.launch {
            _textState
                .debounce(150L)
                .flatMapLatest { text ->
                    annotationPipeline.annotateProgressive(text)
                }
                .collectLatest { annotations ->
                    _annotationsState.value = annotations
                }
        }
    }

    companion object {
        const val SAMPLE_SMS =
            "Hey, meet me at Starbucks on 5th Ave Friday at 3pm - Dr. Sarah Kim"
    }
}
