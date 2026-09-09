package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.enums.LinkStatus;
import com.FirstApiChallenge.api.model.VeterinarianTutorLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeterinarianTutorLinkRepository extends JpaRepository<VeterinarianTutorLink, Long> {

    // Busca solicitação específica entre um veterinário e tutor
    Optional<VeterinarianTutorLink> findByVeterinarianCrmvNumberAndTutorCpf(String crmvNumber, String cpf);

    // Para o tutor ver todas as solicitações pendentes recebidas
    List<VeterinarianTutorLink> findByTutorCpfAndStatus(String cpf, LinkStatus status);

    // Para verificar se um veterinário possui vínculo ACEITO com o tutor
    boolean existsByVeterinarianCpfAndTutorCpfAndStatus(String VeterinarianCpf, String cpf, LinkStatus status);

    // Método para buscar os links aceitos filtrando pelo CPF do Veterinário
    List<VeterinarianTutorLink> findByVeterinarianCpfAndStatus(String veterinarianCpf, LinkStatus status);
}