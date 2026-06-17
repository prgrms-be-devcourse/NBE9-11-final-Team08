package com.team08.backend.domain.studyreport.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStudyReport is a Querydsl query type for StudyReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyReport extends EntityPathBase<StudyReport> {

    private static final long serialVersionUID = -1090817065L;

    public static final QStudyReport studyReport = new QStudyReport("studyReport");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<java.math.BigDecimal> progressRate = createNumber("progressRate", java.math.BigDecimal.class);

    public final NumberPath<Long> studyId = createNumber("studyId", Long.class);

    public final NumberPath<Integer> totalQnaCount = createNumber("totalQnaCount", Integer.class);

    public final NumberPath<Integer> totalWatchTime = createNumber("totalWatchTime", Integer.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QStudyReport(String variable) {
        super(StudyReport.class, forVariable(variable));
    }

    public QStudyReport(Path<? extends StudyReport> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStudyReport(PathMetadata metadata) {
        super(StudyReport.class, metadata);
    }

}

