import type { PayOrderResult } from '@/services/order'

const ECPAY_SIGN_TYPE = 'ECPAY_SHA256'

interface EcpayCheckoutPackage {
  checkoutUrl: string
  params: Record<string, unknown>
}

export function isEcpayPaymentResponse(payment: PayOrderResult | null | undefined) {
  return payment?.signType === ECPAY_SIGN_TYPE
}

export function redirectToEcpayCheckout(payment: PayOrderResult) {
  const checkoutPackage = parseEcpayCheckoutPackage(payment.packageStr)
  const checkoutUrl = new URL(checkoutPackage.checkoutUrl)
  if (!['http:', 'https:'].includes(checkoutUrl.protocol)) {
    throw new Error('付款導轉網址格式錯誤')
  }

  const form = document.createElement('form')
  form.method = 'POST'
  form.action = checkoutUrl.toString()
  form.acceptCharset = 'UTF-8'
  form.style.display = 'none'

  Object.entries(checkoutPackage.params).forEach(([key, value]) => {
    if (value === null || value === undefined) {
      return
    }
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = key
    input.value = String(value)
    form.appendChild(input)
  })

  document.body.appendChild(form)
  form.submit()
}

function parseEcpayCheckoutPackage(packageStr: string | null | undefined): EcpayCheckoutPackage {
  if (!packageStr) {
    throw new Error('付款導轉資料不存在')
  }

  let parsed: unknown
  try {
    parsed = JSON.parse(packageStr)
  } catch {
    throw new Error('付款導轉資料格式錯誤')
  }

  if (!isCheckoutPackage(parsed)) {
    throw new Error('付款導轉資料不完整')
  }
  return parsed
}

function isCheckoutPackage(value: unknown): value is EcpayCheckoutPackage {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Partial<EcpayCheckoutPackage>
  return typeof candidate.checkoutUrl === 'string'
    && !!candidate.checkoutUrl
    && !!candidate.params
    && typeof candidate.params === 'object'
}
