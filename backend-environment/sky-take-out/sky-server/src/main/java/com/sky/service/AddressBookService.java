package com.sky.service;

import com.sky.entity.ShippingAddress;
import java.util.List;

public interface AddressBookService {

    List<ShippingAddress> list(ShippingAddress addressBook);

    void save(ShippingAddress addressBook);

    ShippingAddress getById(Long id);

    void update(ShippingAddress addressBook);

    void setDefault(ShippingAddress addressBook);

    void deleteById(Long id);

}
