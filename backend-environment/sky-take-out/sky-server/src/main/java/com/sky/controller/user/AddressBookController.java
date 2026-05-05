package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.ShippingAddress;
import com.sky.result.Result;
import com.sky.service.ShippingAddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Api(tags = "C端地址簿接口")
public class AddressBookController {

    @Autowired
    private ShippingAddressService shippingAddressService;

    /**
     * 查询当前登录用户的所有地址信息
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询当前登录用户的所有地址信息")
    public Result<List<ShippingAddress>> list() {
        ShippingAddress addressBook = new ShippingAddress();
        addressBook.setMemberId(BaseContext.getCurrentId());
        List<ShippingAddress> list = shippingAddressService.list(addressBook);
        return Result.success(list);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result save(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.save(addressBook);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<ShippingAddress> getById(@PathVariable Long id) {
        ShippingAddress addressBook = shippingAddressService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation("根据id修改地址")
    public Result update(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.update(addressBook);
        return Result.success();
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result setDefault(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 根据id删除地址
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result deleteById(Long id) {
        shippingAddressService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询默认地址
     */
    @GetMapping("default")
    @ApiOperation("查询默认地址")
    public Result<ShippingAddress> getDefault() {
        //SQL:select * from shipping_address where member_id = ? and is_default = 1
        ShippingAddress addressBook = new ShippingAddress();
        addressBook.setIsDefault(1);
        addressBook.setMemberId(BaseContext.getCurrentId());
        List<ShippingAddress> list = shippingAddressService.list(addressBook);

        if (list != null && list.size() == 1) {
            return Result.success(list.get(0));
        }

        return Result.error("没有查询到默认地址");
    }

}
