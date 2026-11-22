package com.munsistore.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munsistore.app.R
import com.munsistore.app.data.ProductQuestion
import com.munsistore.app.data.Review

/** Reviews + Q&A block shown on the product detail page. */
@Composable
fun ReviewsSection(viewModel: StoreViewModel, productId: String, fallbackRating: Float) {
    val reviews by viewModel.reviewsOf(productId).collectAsState(initial = emptyList())
    val questions by viewModel.questionsOf(productId).collectAsState(initial = emptyList())
    var average by remember { mutableStateOf(fallbackRating.toDouble()) }
    var showReviewForm by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var showQuestionForm by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }
    var answerFor by remember { mutableStateOf<ProductQuestion?>(null) }
    var answerText by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(reviews) {
        average = if (reviews.isEmpty()) fallbackRating.toDouble()
        else reviews.sumOf { it.rating.toDouble() } / reviews.size
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reviews),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRating(rating = average)
            Text(
                text = "%.1f".format(average) + " • " + reviews.size + " " + stringResource(R.string.reviews_count),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (reviews.isEmpty()) {
            Text(
                text = stringResource(R.string.no_reviews),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        reviews.take(5).forEach { review ->
            ReviewRow(review = review)
        }
        if (viewModel.isLoggedIn) {
            if (showReviewForm) {
                StarInput(selected = stars, onSelect = { stars = it })
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text(stringResource(R.string.write_review)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    if (reviewText.isBlank()) return@Button
                    viewModel.addReview(productId, stars, reviewText, "")
                    reviewText = ""
                    showReviewForm = false
                }) {
                    Text(stringResource(R.string.submit_review))
                }
            } else {
                OutlinedButton(onClick = { showReviewForm = true }) {
                    Text(stringResource(R.string.write_review))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.questions_answers),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (questions.isEmpty()) {
            Text(
                text = stringResource(R.string.no_questions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        questions.take(5).forEach { q ->
            QuestionRow(
                question = q,
                canAnswer = viewModel.isAdmin,
                onAnswerClick = { answerFor = q; answerText = q.answer ?: "" }
            )
        }
        if (viewModel.isLoggedIn) {
            if (showQuestionForm) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text(stringResource(R.string.ask_question)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    if (questionText.isBlank()) return@Button
                    viewModel.askQuestion(productId, questionText)
                    questionText = ""
                    showQuestionForm = false
                }) {
                    Text(stringResource(R.string.submit_question))
                }
            } else {
                OutlinedButton(onClick = { showQuestionForm = true }) {
                    Text(stringResource(R.string.ask_question))
                }
            }
        }
        val answering = answerFor
        if (answering != null) {
            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                label = { Text(stringResource(R.string.answer)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (answerText.isBlank()) return@Button
                    viewModel.answerQuestion(answering.id, answerText)
                    answerFor = null
                }) {
                    Text(stringResource(R.string.save))
                }
                TextButton(onClick = { answerFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(review: Review) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRating(rating = review.rating.toDouble())
                Text(
                    text = review.userName,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (review.text.isNotBlank()) {
                Text(text = review.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun QuestionRow(
    question: ProductQuestion,
    canAnswer: Boolean,
    onAnswerClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Q: " + question.question,
                fontWeight = FontWeight.SemiBold
            )
            if (!question.answer.isNullOrBlank()) {
                Text(
                    text = "A: " + question.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.waiting_answer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canAnswer) {
                TextButton(onClick = onAnswerClick) {
                    Text(stringResource(R.string.answer))
                }
            }
        }
    }
}
