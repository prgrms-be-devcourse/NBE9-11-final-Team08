type TossPaymentRequest = {
  method: 'CARD'
  amount: {
    currency: 'KRW'
    value: number
  }
  orderId: string
  orderName: string
  successUrl: string
  failUrl: string
  card?: {
    flowMode?: 'DEFAULT' | 'DIRECT'
    easyPay?: string
  }
}

type TossPaymentInstance = {
  requestPayment: (request: TossPaymentRequest) => Promise<void>
}

type TossPaymentsClient = {
  payment: (params: { customerKey: string }) => TossPaymentInstance
}

type TossPaymentsInitializer = {
  (clientKey: string): TossPaymentsClient
  ANONYMOUS: string
}

declare global {
  interface Window {
    TossPayments?: TossPaymentsInitializer
  }
}

const TOSS_SDK_URL = 'https://js.tosspayments.com/v2/standard'

let sdkPromise: Promise<TossPaymentsInitializer> | null = null

export function loadTossPayments() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Toss Payments SDK is only available in the browser.'))
  }

  if (window.TossPayments) {
    return Promise.resolve(window.TossPayments)
  }

  if (sdkPromise) return sdkPromise

  sdkPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${TOSS_SDK_URL}"]`)
    if (existingScript) {
      existingScript.addEventListener('load', () => {
        if (window.TossPayments) resolve(window.TossPayments)
        else reject(new Error('Toss Payments SDK failed to initialize.'))
      })
      existingScript.addEventListener('error', () => reject(new Error('Toss Payments SDK failed to load.')))
      return
    }

    const script = document.createElement('script')
    script.src = TOSS_SDK_URL
    script.async = true
    script.onload = () => {
      if (window.TossPayments) resolve(window.TossPayments)
      else reject(new Error('Toss Payments SDK failed to initialize.'))
    }
    script.onerror = () => reject(new Error('Toss Payments SDK failed to load.'))
    document.head.appendChild(script)
  })

  return sdkPromise
}
