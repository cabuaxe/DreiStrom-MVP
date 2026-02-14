package de.dreistrom.income.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "client")
@Getter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(name = "ust_id_nr", length = 20)
    private String ustIdNr;

    @Column(nullable = false, length = 2)
    private String country = "DE";

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType = ClientType.B2B;

    @Enumerated(EnumType.STRING)
    @Column(name = "stream_type", nullable = false)
    private IncomeStream streamType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public Client(AppUser user, String name, IncomeStream streamType) {
        this.user = user;
        this.name = name;
        this.streamType = streamType;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Client(AppUser user, String name, IncomeStream streamType,
                  ClientType clientType, String country, String ustIdNr) {
        this(user, name, streamType);
        this.clientType = clientType;
        this.country = country;
        this.ustIdNr = ustIdNr;
    }
}
