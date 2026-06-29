package com.team08.backend.domain.cart.service;

import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartCreationService {

    private final CartRepository cartRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long userId) {
        cartRepository.saveAndFlush(Cart.create(userId));
    }
}
