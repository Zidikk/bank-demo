package ru.quipy.sagaApp.payment.subscribers

import org.springframework.stereotype.Component

import ru.quipy.core.EventSourcingService
import ru.quipy.saga.SagaManager
import ru.quipy.sagaApp.payment.api.PaymentAggregate
import ru.quipy.sagaApp.payment.logic.Payment
import ru.quipy.sagaApp.trips.api.TripAggregate
import ru.quipy.sagaApp.trips.api.TripReservationStartedEvent
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct

@Component
class TripSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val paymentEsService: EventSourcingService<UUID, PaymentAggregate, Payment>,
    private val sagaManager: SagaManager
) {

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(TripAggregate::class, "payment::trips-subscriber") {
            `when`(TripReservationStartedEvent::class) { event ->
                val sagaContext = sagaManager
                    .withContextGiven(event.sagaContext)
                    .launchSaga("TRIP_RESERVATION2", "process payment2")
                    .performSagaStep("TRIP_RESERVATION", "process payment")
                    .sagaContext

                paymentEsService.create(sagaContext) { it.processPayment(event.tripId,100) }
            }
        }
    }
}