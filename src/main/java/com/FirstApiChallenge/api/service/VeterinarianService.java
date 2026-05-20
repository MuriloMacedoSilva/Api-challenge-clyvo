package com.FirstApiChallenge.api.service;

import com.FirstApiChallenge.api.dto.VeterinarianRequestDTO;
import com.FirstApiChallenge.api.dto.VeterinarianResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import com.FirstApiChallenge.api.model.Veterinarian;
import com.FirstApiChallenge.api.repository.VeterinarianRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class VeterinarianService {

    private final VeterinarianRepository veterinarianRepository;

    public VeterinarianService(VeterinarianRepository veterinarianRepository) {
        this.veterinarianRepository = veterinarianRepository;
    }

    @Transactional(readOnly = true)
    public Optional<VeterinarianResponseDTO> searchVeterinarianByCpf(String cpf) {
        return veterinarianRepository.findByCpf(cpf)
                .map(VeterinarianResponseDTO::fromEntity);
    }

    @Transactional
    public VeterinarianResponseDTO authenticateVeterinarian(VeterinarianRequestDTO requestDTO) {
        Veterinarian veterinarian = veterinarianRepository.findByCpf(requestDTO.cpf())
                .orElseThrow(() -> new CustomException("Veterinario não encontrado", HttpStatus.NOT_FOUND));

        if (!veterinarian.getPassword().equals(requestDTO.password())){
            throw new CustomException("Senha incorreta", HttpStatus.UNAUTHORIZED);
        }

        return VeterinarianResponseDTO.fromEntity(veterinarian);
    }


    @Transactional
    public VeterinarianResponseDTO createVeterinarian(VeterinarianRequestDTO requestDTO) {

        if (veterinarianRepository.findByCpf(requestDTO.cpf()).isPresent()) {
            throw new CustomException("CPF já cadastrado", HttpStatus.CONFLICT);
        }

        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setName(requestDTO.name());
        veterinarian.setEmail(requestDTO.email());
        veterinarian.setCpf(requestDTO.cpf());
        veterinarian.setCnpj(requestDTO.cnpj());
        veterinarian.setPassword(requestDTO.password());
        veterinarian.setPhoneNumber(requestDTO.phoneNumber());
        veterinarian.setCrmvNumber(requestDTO.crmvNumber());
        veterinarian.setCrmvState(requestDTO.crmvState());
        veterinarian.setRole(requestDTO.role());

        Veterinarian veterinarianSaved = veterinarianRepository.save(veterinarian);

        return VeterinarianResponseDTO.fromEntity(veterinarianSaved);

    }


    @Transactional
    public Optional<VeterinarianResponseDTO> updateVeterinarianByCpf(String cpf, VeterinarianRequestDTO requestDTO) {
        return veterinarianRepository.findByCpf(cpf).map(veterinarianExistent -> {
            veterinarianExistent.setName(requestDTO.name());
            veterinarianExistent.setEmail(requestDTO.email());
            veterinarianExistent.setCpf(requestDTO.cpf());
            veterinarianExistent.setCnpj(requestDTO.cnpj());
            veterinarianExistent.setPassword(requestDTO.password());
            veterinarianExistent.setPhoneNumber(requestDTO.phoneNumber());
            veterinarianExistent.setCrmvNumber(requestDTO.crmvNumber());
            veterinarianExistent.setCrmvState(requestDTO.crmvState());
            veterinarianExistent.setRole(requestDTO.role());

            Veterinarian veterinarianUpdated = veterinarianRepository.save(veterinarianExistent);

            return VeterinarianResponseDTO.fromEntity(veterinarianUpdated);

        });
    }



}



//package com.FirstApiChallenge.api.service;
//
//import com.FirstApiChallenge.api.dto.VeterinarianRequestDTO;
//import com.FirstApiChallenge.api.dto.VeterinarianResponseDTO;
//import com.FirstApiChallenge.api.exception.CustomException;
//import com.FirstApiChallenge.api.model.Veterinarian;
//import com.FirstApiChallenge.api.repository.VeterinarianRepository;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
//@Service
//public class VeterinarianService {
//
//    private final VeterinarianRepository veterinarianRepository;
//
//    public VeterinarianService(VeterinarianRepository veterinarianRepository) {
//        this.veterinarianRepository = veterinarianRepository;
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<VeterinarianResponseDTO> searchVeterinarianByCpf(String cpf) {
//        return veterinarianRepository.findByCpf(cpf)
//                .map(VeterinarianResponseDTO::fromEntity);
//    }
//
//    @Transactional(readOnly = true) // Pode ser readOnly aqui, já que não altera o banco
//    public VeterinarianResponseDTO authenticateVeterinarian(VeterinarianRequestDTO requestDTO) {
//        Veterinarian veterinarian = veterinarianRepository.findByCpf(requestDTO.cpf())
//                .orElseThrow(() -> new CustomException("Veterinario não encontrado", HttpStatus.NOT_FOUND));
//
//        // TODO: No futuro, usar BCryptPasswordEncoder aqui
//        if (!veterinarian.getPassword().equals(requestDTO.password())){
//            throw new CustomException("Senha incorreta", HttpStatus.UNAUTHORIZED);
//        }
//
//        return VeterinarianResponseDTO.fromEntity(veterinarian);
//    }
//
//    @Transactional
//    public VeterinarianResponseDTO createVeterinarian(VeterinarianRequestDTO requestDTO) {
//        if (veterinarianRepository.findByCpf(requestDTO.cpf()).isPresent()) {
//            throw new CustomException("CPF já cadastrado", HttpStatus.CONFLICT);
//        }
//
//        Veterinarian veterinarian = new Veterinarian();
//        veterinarian.setName(requestDTO.name());
//        veterinarian.setEmail(requestDTO.email());
//        veterinarian.setCpf(requestDTO.cpf());
//        veterinarian.setCnpj(requestDTO.cnpj());
//        veterinarian.setPassword(requestDTO.password()); // TODO: passwordEncoder.encode(...)
//        veterinarian.setPhoneNumber(requestDTO.phoneNumber());
//        veterinarian.setCrmvNumber(requestDTO.crmvNumber());
//        veterinarian.setCrmvState(requestDTO.crmvState());
//        veterinarian.setRole(requestDTO.role());
//
//        Veterinarian veterinarianSaved = veterinarianRepository.save(veterinarian);
//        return VeterinarianResponseDTO.fromEntity(veterinarianSaved);
//    }
//
//    @Transactional
//    public Optional<VeterinarianResponseDTO> updateVeterinarianByCpf(String cpf, VeterinarianRequestDTO requestDTO) {
//        return veterinarianRepository.findByCpf(cpf).map(veterinarianExistent -> {
//
//            // Blinda contra alteração de CPF para um que já existe em OUTRO cadastro
//            if (!veterinarianExistent.getCpf().equals(requestDTO.cpf()) &&
//                    veterinarianRepository.findByCpf(requestDTO.cpf()).isPresent()) {
//                throw new CustomException("O novo CPF informado já está em uso", HttpStatus.CONFLICT);
//            }
//
//            veterinarianExistent.setName(requestDTO.name());
//            veterinarianExistent.setEmail(requestDTO.email());
//            veterinarianExistent.setCpf(requestDTO.cpf()); // Se o CPF for imutável, remova esta linha
//            veterinarianExistent.setCnpj(requestDTO.cnpj());
//            veterinarianExistent.setPassword(requestDTO.password());
//            veterinarianExistent.setPhoneNumber(requestDTO.phoneNumber());
//            veterinarianExistent.setCrmvNumber(requestDTO.crmvNumber());
//            veterinarianExistent.setCrmvState(requestDTO.crmvState());
//            veterinarianExistent.setRole(requestDTO.role());
//
//            // O .save() foi removido daqui pois o Dirty Checking do Hibernate cuidará do update automaticamente no commit da transação.
//            return VeterinarianResponseDTO.fromEntity(veterinarianExistent);
//        });
//    }
//}
