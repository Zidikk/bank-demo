package ru.quipy.sagaTest.aggregate.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType("sagas")
class SagaStepAggregate : Aggregate