import type { NicepayPreparePaymentResponse } from './types'

export type NicepayAuthResult = {
  AuthResultCode?: string
  AuthResultMsg?: string
  AuthToken?: string
  TxTid?: string
  TID?: string
  tid?: string
  MID?: string
  Moid?: string
  moid?: string
  Amt?: string
  amt?: string
  Signature?: string
  NextAppURL?: string
  NetCancelURL?: string
  PayMethod?: string
  EasyPayCl?: string
  ClickpayCl?: string
  EasyPayMethod?: string
  SelectPayMethod?: string
}

declare global {
  interface Window {
    goPay?: (form: HTMLFormElement) => void
    nicepaySubmit?: () => void
    nicepayClose?: () => void
  }
}

const NICEPAY_PC_SCRIPT_URL = 'https://pg-web.nicepay.co.kr/v3/common/js/nicepay-pgweb.js'

export async function loadNicepayPcScript() {
  if (typeof window === 'undefined') {
    throw new Error('NICEPAY 결제창은 브라우저에서만 호출할 수 있습니다.')
  }

  if (window.goPay) {
    return
  }

  await new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${NICEPAY_PC_SCRIPT_URL}"]`)
    if (existing) {
      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error('NICEPAY 결제창 스크립트 로드에 실패했습니다.')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = NICEPAY_PC_SCRIPT_URL
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('NICEPAY 결제창 스크립트 로드에 실패했습니다.'))
    document.head.appendChild(script)
  })

  if (!window.goPay) {
    throw new Error('NICEPAY 결제창을 사용할 수 없습니다.')
  }
}

export async function requestNicepayPayment(prepare: NicepayPreparePaymentResponse) {
  await loadNicepayPcScript()

  return new Promise<NicepayAuthResult>((resolve, reject) => {
    const form = createNicepayForm(prepare)

    const cleanup = () => {
      form.remove()
      cleanupNicepayLayers()
      delete window.nicepaySubmit
      delete window.nicepayClose
    }

    window.nicepaySubmit = () => {
      const result = collectFormData(form)
      cleanup()
      resolve(result)
    }
    window.nicepayClose = () => {
      cleanup()
      reject(new Error('NICEPAY 결제창이 닫혔습니다.'))
    }

    document.body.appendChild(form)
    window.goPay?.(form)
  })
}

function createNicepayForm(prepare: NicepayPreparePaymentResponse) {
  const form = document.createElement('form')
  form.name = 'payForm'
  form.method = 'post'
  form.acceptCharset = prepare.charSet || 'utf-8'
  form.style.display = 'none'

  const fields: Record<string, string> = {
    GoodsName: prepare.goodsName,
    Amt: String(prepare.amt),
    MID: prepare.mid,
    EdiDate: prepare.ediDate,
    Moid: prepare.moid,
    SignData: prepare.signData,
    PayMethod: prepare.payMethod,
    BuyerName: prepare.buyerName,
    BuyerTel: prepare.buyerTel,
    BuyerEmail: prepare.buyerEmail,
    CharSet: prepare.charSet || 'utf-8',
  }

  if (prepare.reqReserved) {
    fields.ReqReserved = prepare.reqReserved
  }

  Object.entries(fields).forEach(([name, value]) => {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  })

  return form
}

function collectFormData(form: HTMLFormElement) {
  const formData = new FormData(form)
  return Object.fromEntries(Array.from(formData.entries()).map(([key, value]) => [key, String(value)])) as NicepayAuthResult
}

function cleanupNicepayLayers() {
  const selectors = [
    '#nice_layer',
    '#nicepay_layer',
    '#nicepayLayer',
    '#nicepayOverlay',
    '.nicepay-layer',
    '.nicepay-overlay',
  ]

  selectors.forEach((selector) => {
    document.querySelectorAll(selector).forEach((element) => element.remove())
  })
}
