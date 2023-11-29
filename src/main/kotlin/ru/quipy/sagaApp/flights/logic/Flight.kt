package ru.quipy.sagaApp.flights.logic

import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.sagaApp.flights.api.FlightAggregate
import ru.quipy.sagaApp.flights.api.FlightReservationCanceledEvent
import ru.quipy.sagaApp.flights.api.FlightReservedEvent
import java.util.*

class Flight : AggregateState<UUID, FlightAggregate> {
    private lateinit var flightId: UUID
    private var canceled: Boolean = false

    override fun getId() = flightId

    fun reserveFlight(id: UUID = UUID.randomUUID()): FlightReservedEvent {
        return FlightReservedEvent(id)
    }

    fun canselFlight(id: UUID): FlightReservationCanceledEvent {
        return FlightReservationCanceledEvent(id)
    }

    @StateTransitionFunc
    fun reserveFlight(event: FlightReservedEvent) {
        flightId = event.flightReservationId
    }

    @StateTransitionFunc
    fun canselFlight(event: FlightReservationCanceledEvent) {
        canceled = true
    }
}
