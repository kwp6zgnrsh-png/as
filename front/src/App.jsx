import { useEffect, useRef, useState } from 'react'
import './App.css'

const regions = [
  { code: 'SEOUL', name: '서울특별시' },
  { code: 'BUSAN', name: '부산광역시' },
]

function getLocalDateTime() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function App() {
  const privacyDialog = useRef(null)
  const submissionInFlight = useRef(false)
  const [notice, setNotice] = useState('')
  const [noticeType, setNoticeType] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [products, setProducts] = useState([])
  const [productStatus, setProductStatus] = useState('loading')

  useEffect(() => {
    const controller = new AbortController()

    async function loadProducts() {
      try {
        // Vite 프록시를 통해 로컬 AS 접수 백엔드의 GET /api/products를 호출합니다.
        const response = await fetch('/api/products', { signal: controller.signal })
        if (!response.ok) throw new Error('제품 목록 조회 실패')

        const data = await response.json()
        if (!Array.isArray(data) || data.some((product) => (
          !product || product.productId == null ||
          typeof product.productName !== 'string' || !product.productName.trim()
        ))) {
          throw new Error('제품 목록 응답 형식 오류')
        }

        if (!controller.signal.aborted) {
          setProducts(data)
          setProductStatus('success')
        }
      } catch {
        if (!controller.signal.aborted) setProductStatus('error')
      }
    }

    loadProducts()
    return () => controller.abort()
  }, [])

  const productPlaceholder = productStatus === 'loading'
    ? '제품 목록을 불러오는 중입니다.'
    : productStatus === 'error'
      ? '제품 목록을 불러오지 못했습니다.'
      : products.length === 0 ? '등록된 제품이 없습니다.' : '제품을 선택하세요'

  async function handleSubmit(event) {
    event.preventDefault()
    if (submissionInFlight.current) return

    const form = event.currentTarget
    const visitDateTime = form.elements.visitDateTime
    const visit = new Date(visitDateTime.value)
    // 페이지를 오래 열어 둔 경우에도 지난 시간으로 신청하지 않도록 확인합니다.
    if (!Number.isFinite(visit.getTime()) || visit.getTime() <= Date.now()) {
      visitDateTime.setCustomValidity('현재 시간 이후의 희망 방문 일시를 선택해 주세요.')
      visitDateTime.reportValidity()
      return
    }

    const minutes = visit.getHours() * 60 + visit.getMinutes()
    const morning = minutes >= 9 * 60 && minutes < 12 * 60
    const afternoon = minutes >= 13 * 60 && minutes < 18 * 60
    if (!morning && !afternoon) {
      visitDateTime.setCustomValidity('방문 시간은 09:00 이상 12:00 미만 또는 13:00 이상 18:00 미만으로 선택해 주세요.')
      visitDateTime.reportValidity()
      return
    }
    const values = new FormData(form)
    const request = {
      productId: Number(values.get('product')),
      customerName: values.get('customerName').trim(),
      phone: values.get('phone').trim(),
      region: values.get('region'),
      visitDateTime: values.get('visitDateTime'),
      memo: values.get('memo').trim(),
    }

    if (!Number.isSafeInteger(request.productId) || request.productId <= 0) {
      setNotice('제품을 선택해 주세요.')
      setNoticeType('error')
      return
    }

    submissionInFlight.current = true
    setIsSubmitting(true)
    setNotice('신청 내용을 전송하고 있습니다.')
    setNoticeType('')

    try {
      const response = await fetch('/api/repairs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      })
      const result = await response.json().catch(() => null)

      if (!response.ok) {
        const message = typeof result?.message === 'string' ? result.message : result?.detail
        throw new Error(typeof message === 'string' && message
          ? message
          : `AS 신청을 처리하지 못했습니다. (오류 ${response.status})`)
      }

      form.reset()
      for (const element of form.elements) {
        element.setCustomValidity?.('')
      }
      setNotice('AS 접수가 완료되었습니다.')
      setNoticeType('success')
    } catch (error) {
      setNotice(error instanceof TypeError
        ? '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
        : error.message || 'AS 신청 중 오류가 발생했습니다.')
      setNoticeType('error')
    } finally {
      submissionInFlight.current = false
      setIsSubmitting(false)
    }
  }

  return (
    <main className="application-page">
      <h1 className="sr-only">가전제품 AS 상담 신청</h1>
      <form className="application-form" onSubmit={handleSubmit} onChange={() => {
        setNotice('')
        setNoticeType('')
      }}>
        <div className="form-fields">
          <div className="form-row">
            <label className="field-label" htmlFor="product">제품분류</label>
            <div className="field-content">
              <div className="select-wrap product-select">
                <select id="product" name="product" defaultValue="" required disabled={products.length === 0} aria-busy={productStatus === 'loading'}>
                  <option value="" disabled>{productPlaceholder}</option>
                  {products.map((product) => (
                    <option key={product.productId} value={product.productId}>{product.productName}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          <div className="form-row">
            <label className="field-label" htmlFor="customerName">신청자명</label>
            <div className="field-content">
              <input className="short-input" id="customerName" name="customerName" type="text" autoComplete="name" minLength={2} maxLength={8} required
                onChange={(event) => {
                  const length = event.target.value.trim().length
                  event.target.setCustomValidity(length >= 2 && length <= 8 ? '' : '신청자명은 2자 이상 8자 이하로 입력해 주세요.')
                }} />
            </div>
          </div>
          <div className="form-row">
            <label className="field-label" htmlFor="phone">연락처</label>
            <div className="field-content">
              <input className="short-input" id="phone" name="phone" type="tel" inputMode="numeric" autoComplete="tel-national"
                placeholder="010으로 시작하는 11자리 숫자" pattern="010[0-9]{8}"
                title="연락처는 하이픈 없이 010으로 시작하는 11자리 숫자로 입력해 주세요." maxLength={11} required />
            </div>
          </div>
          <div className="form-row">
            <label className="field-label" htmlFor="region">지역</label>
            <div className="field-content">
              <div className="select-wrap region-select">
                <select id="region" name="region" defaultValue="" required>
                  <option value="" disabled>시도 선택</option>
                  {regions.map((region) => <option key={region.code} value={region.code}>{region.name}</option>)}
                </select>
              </div>
            </div>
          </div>
          <div className="form-row">
            <label className="field-label" htmlFor="visitDateTime">희망 방문 일시</label>
            <div className="field-content">
              <input className="datetime-input" id="visitDateTime" name="visitDateTime" type="datetime-local" min={getLocalDateTime()} required
                onChange={(event) => event.target.setCustomValidity('')} />
            </div>
          </div>
          <div className="form-row memo-row">
            <label className="field-label" htmlFor="memo">고장 증상</label>
            <div className="field-content">
              <textarea id="memo" name="memo" maxLength={1000} aria-label="메모사항 (고장 증상 및 요청사항)" />
            </div>
          </div>
        </div>
        <div className="form-actions">
          <div className="privacy-agreement">
            <label className="agreement-label">
              <input type="checkbox" name="privacyAgreement" required />
              <span>개인정보 취급방침에 동의함</span>
            </label>
            <button className="privacy-button" type="button" onClick={() => privacyDialog.current.showModal()}>[전문보기]</button>
          </div>
          <button className="submit-button" type="submit" disabled={products.length === 0 || isSubmitting} aria-busy={isSubmitting}>
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
              <path d="M14 5H6a2 2 0 0 0-2 2v21a2 2 0 0 0 2 2h20a2 2 0 0 0 2-2V17" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
              <path d="m13 22 2-7L26 4a2.8 2.8 0 0 1 4 4L19 19l-6 3ZM23 7l4 4M15 15l4 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            {isSubmitting ? '신청 중...' : '신청하기'}
          </button>
          <p className="submission-notice" data-status={noticeType} role="status">{notice}</p>
        </div>
      </form>
      <dialog className="privacy-dialog" ref={privacyDialog} aria-labelledby="privacy-title" onClick={(event) => {
        if (event.target === event.currentTarget) privacyDialog.current.close()
      }}>
        <h2 id="privacy-title">개인정보 수집 및 이용 안내</h2>
        <p className="privacy-draft">화면 개발용 안내입니다. 서비스 운영 전 실제 개인정보 처리방침으로 교체해야 합니다.</p>
        <dl>
          <dt>수집 목적</dt><dd>AS 상담 접수 및 방문 일정 안내</dd>
          <dt>수집 항목</dt><dd>신청자명, 연락처, 지역, 희망 방문 일시, 메모사항</dd>
        </dl>
        <p>입력하신 정보는 AS 상담 접수와 방문 일정 안내를 위해 서버로 전송됩니다. 메모에는 상담에 필요한 내용만 입력해 주세요.</p>
        <form method="dialog"><button className="dialog-close" autoFocus>닫기</button></form>
      </dialog>
    </main>
  )
}

export default App
