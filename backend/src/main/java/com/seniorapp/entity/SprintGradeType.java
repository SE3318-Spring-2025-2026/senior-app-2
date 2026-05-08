package com.seniorapp.entity;

/**
 * Type of advisor grade per sprint as defined by the grading spec.
 *
 * <p>Point A = Scrum (team performance, attendance, backlog management).
 * Point B = CodeReview (work quality, pull-request review quality).
 */
public enum SprintGradeType {
    /** Point A - Advisor grades overall scrum/team performance. */
    SCRUM,
    /** Point B - Advisor grades code/work review quality. */
    CODE_REVIEW
}
