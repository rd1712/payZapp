package com.payzapp.walletservice.service;

import com.payzapp.walletservice.dto.WalletResponse;
import com.payzapp.walletservice.exception.WalletAlreadyExistsException;
import com.payzapp.walletservice.exception.WalletDoesntExistException;
import com.payzapp.walletservice.model.Wallet;
import com.payzapp.walletservice.model.WalletStatus;
import com.payzapp.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletResponse createWallet(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException("Wallet Already Exists");
        }

            Wallet wallet = walletRepository.save(Wallet.builder()
                    .userId(userId)
                    .status(WalletStatus.ACTIVE)
                    .balance(BigDecimal.ZERO)
                    .build());

            return WalletResponse.builder()
                    .walletId(wallet.getWalletId())
                    .userId(wallet.getUserId())
                    .balance(wallet.getBalance())
                    .status(wallet.getStatus())
                    .build();



    }

    public BigDecimal getBalance(UUID userId){
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(()-> new WalletDoesntExistException("Wallet does not exist"));

        return wallet.getBalance();

    }
}
