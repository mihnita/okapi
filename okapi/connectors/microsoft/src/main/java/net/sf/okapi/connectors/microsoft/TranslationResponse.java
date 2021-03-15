package net.sf.okapi.connectors.microsoft;

import net.sf.okapi.common.query.QueryResult;

class TranslationResponse {
	public int matchDegree;
	public int rating;
	public int combinedScore;
	public String sourceText;
	public String translatedText;

	TranslationResponse(String sourceText, String translatedText, int rating, int matchDegree) {
		this.sourceText = sourceText;
		this.translatedText = translatedText;
		this.rating = rating;
		this.matchDegree = matchDegree;
		this.combinedScore = calculateCombinedScore();
	}
	private int calculateCombinedScore() {
		if (rating==QueryResult.QUALITY_UNDEFINED) {
			return QueryResult.COMBINEDSCORE_UNDEFINED;
		}
		int combinedScore = matchDegree;
		if ( combinedScore > 90 ) {
			combinedScore += (rating - 10);
			// Ideally we would want a composite value for the score
		}
		return combinedScore;
	}
}