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
@RequestMapping("/user/shippingAddress")
@Api(tags = "會員端收貨地址介面")
public class ShippingAddressController {

    @Autowired
    private ShippingAddressService shippingAddressService;

    /**
     * 查詢目前登入會員的所有收貨地址
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查詢目前登入會員的所有收貨地址")
    public Result<List<ShippingAddress>> list() {
        ShippingAddress addressBook = new ShippingAddress();
        addressBook.setMemberId(BaseContext.getCurrentId());
        List<ShippingAddress> list = shippingAddressService.list(addressBook);
        return Result.success(list);
    }

    /**
     * 新增收貨地址
     *
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation("新增收貨地址")
    public Result save(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.save(addressBook);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根據 ID 查詢收貨地址")
    public Result<ShippingAddress> getById(@PathVariable Long id) {
        ShippingAddress addressBook = shippingAddressService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根據 ID 修改收貨地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation("根據 ID 修改收貨地址")
    public Result update(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.update(addressBook);
        return Result.success();
    }

    /**
     * 設定預設收貨地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("設定預設收貨地址")
    public Result setDefault(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 根據 ID 刪除收貨地址
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根據 ID 刪除收貨地址")
    public Result deleteById(Long id) {
        shippingAddressService.deleteById(id);
        return Result.success();
    }

    /**
     * 查詢預設收貨地址
     */
    @GetMapping("default")
    @ApiOperation("查詢預設收貨地址")
    public Result<ShippingAddress> getDefault() {
        //SQL:select * from shipping_address where member_id = ? and is_default = 1
        ShippingAddress addressBook = new ShippingAddress();
        addressBook.setIsDefault(1);
        addressBook.setMemberId(BaseContext.getCurrentId());
        List<ShippingAddress> list = shippingAddressService.list(addressBook);

        if (list != null && list.size() == 1) {
            return Result.success(list.get(0));
        }

        return Result.error("沒有查詢到預設地址");
    }

}
