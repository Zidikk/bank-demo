package ru.quipy.sagaApp.payment.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import ru.quipy.sagaApp.payment.api.PaymentAggregate
import ru.quipy.sagaApp.payment.logic.Payment
import java.util.*

@Configuration
class PaymentBoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun paymentEsService(): EventSourcingService<UUID, PaymentAggregate, Payment> =
        eventSourcingServiceFactory.create()
}