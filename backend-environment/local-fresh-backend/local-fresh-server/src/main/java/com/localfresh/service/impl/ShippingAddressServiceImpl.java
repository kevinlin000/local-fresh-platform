package com.localfresh.service.impl;

import com.localfresh.context.BaseContext;
import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.exception.AddressBookBusinessException;
import com.localfresh.mapper.ShippingAddressMapper;
import com.localfresh.service.ShippingAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Slf4j
public class ShippingAddressServiceImpl implements ShippingAddressService {
    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    /**
     * 條件查詢
     *
     * @param addressBook
     * @return
     */
    public List<ShippingAddress> list(ShippingAddress addressBook) {
        return shippingAddressMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(ShippingAddress addressBook) {
        addressBook.setMemberId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        shippingAddressMapper.insert(addressBook);
    }

    /**
     * 根據id查詢
     *
     * @param id
     * @return
     */
    public ShippingAddress getById(Long id) {
        ShippingAddress addressBook = shippingAddressMapper.getById(id);
        checkAddressOwner(addressBook);
        return addressBook;
    }

    /**
     * 根據id修改地址
     *
     * @param addressBook
     */
    public void update(ShippingAddress addressBook) {
        ShippingAddress addressBookDB = shippingAddressMapper.getById(addressBook.getId());
        checkAddressOwner(addressBookDB);
        shippingAddressMapper.update(addressBook);
    }

    /**
     * 設定預設地址
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(ShippingAddress addressBook) {
        ShippingAddress addressBookDB = shippingAddressMapper.getById(addressBook.getId());
        checkAddressOwner(addressBookDB);

        //1、将目前会员的所有地址修改为非預設地址
        addressBook.setIsDefault(0);
        addressBook.setMemberId(BaseContext.getCurrentId());
        shippingAddressMapper.updateIsDefaultByUserId(addressBook);

        //2、将目前地址改为預設地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        shippingAddressMapper.update(addressBook);
    }

    /**
     * 根據id刪除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        ShippingAddress addressBook = shippingAddressMapper.getById(id);
        checkAddressOwner(addressBook);
        shippingAddressMapper.deleteById(id);
    }

    private void checkAddressOwner(ShippingAddress addressBook) {
        Long currentMemberId = BaseContext.getCurrentId();
        if (addressBook == null || !currentMemberId.equals(addressBook.getMemberId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
    }

}
