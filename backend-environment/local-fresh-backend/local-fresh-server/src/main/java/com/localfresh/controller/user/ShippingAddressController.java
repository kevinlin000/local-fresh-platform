package com.localfresh.controller.user;

import com.localfresh.context.BaseContext;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.result.Result;
import com.localfresh.service.ShippingAddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user/shippingAddress")
@Tag(name = "會員端配送地址介面")
public class ShippingAddressController {

    @Autowired
    private ShippingAddressService shippingAddressService;

    /**
     * 查詢目前登入會員的所有配送地址
     *
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查詢目前登入會員的所有配送地址")
    public Result<List<ShippingAddress>> list() {
        ShippingAddress addressBook = new ShippingAddress();
        addressBook.setMemberId(BaseContext.getCurrentId());
        List<ShippingAddress> list = shippingAddressService.list(addressBook);
        return Result.success(list);
    }

    /**
     * 新增配送地址
     *
     * @param addressBook
     * @return
     */
    @PostMapping
    @Operation(summary = "新增配送地址")
    public Result save(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.save(addressBook);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根據 ID 查詢配送地址")
    public Result<ShippingAddress> getById(@PathVariable Long id) {
        ShippingAddress addressBook = shippingAddressService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根據 ID 修改配送地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping
    @Operation(summary = "根據 ID 修改配送地址")
    public Result update(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.update(addressBook);
        return Result.success();
    }

    /**
     * 設定預設配送地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @Operation(summary = "設定預設配送地址")
    public Result setDefault(@RequestBody ShippingAddress addressBook) {
        shippingAddressService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 根據 ID 刪除配送地址
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @Operation(summary = "根據 ID 刪除配送地址")
    public Result deleteById(Long id) {
        shippingAddressService.deleteById(id);
        return Result.success();
    }

    /**
     * 查詢預設配送地址
     */
    @GetMapping("default")
    @Operation(summary = "查詢預設配送地址")
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
