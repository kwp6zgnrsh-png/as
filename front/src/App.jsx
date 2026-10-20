import { useEffect, useRef, useState } from 'react'
import './App.css'

const regions = ['서울특별시', '부산광역시']

function getLocalDateTime() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function App() {
  const privacyDialog = useRef(null)
  const [notice, setNotice] = useState('')
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

  function handleSubmit(event) {
    event.preventDefault()
    const visitDateTime = event.currentTarget.elements.visitDateTime
    // 페이지를 오래 열어 둔 경우에도 지난 시간으로 신청하지 않도록 확인합니다.
    if (new Date(visitDateTime.value).getTime() <= Date.now()) {
      visitDateTime.setCustomValidity('현재 시간 이후의 희망 방문 일시를 선택해 주세요.')
      visitDateTime.reportValidity()
      return
    }
    setNotice('입력 내용을 확인했습니다. 현재는 화면 구현 단계로, 실제 AS 접수는 전송되지 않습니다.')
  }

  return (
    <main className="application-page">
      <h1 className="sr-only">가전제품 AS 상담 신청</h1>
      <form className="application-form" onSubmit={handleSubmit} onChange={() => setNotice('')}>
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
              <input className="short-input" id="customerName" name="customerName" type="text" autoComplete="name" maxLength={50} required
                onChange={(event) => event.target.setCustomValidity(event.target.value.trim() ? '' : '신청자명을 입력해 주세요.')} />
            </div>
          </div>
          <div className="form-row">
            <label className="field-label" htmlFor="phone">연락처</label>
            <div className="field-content">
              <input className="short-input" id="phone" name="phone" type="tel" inputMode="numeric" autoComplete="tel-national"
                placeholder="연락처를 '-' 제외하고 입력하세요." pattern="0[0-9]{8,10}"
                title="0으로 시작하는 9~11자리 연락처를 숫자만 입력해 주세요." maxLength={11} required />
            </div>
          </div>
          <div className="form-row">
            <label className="field-label" htmlFor="region">지역</label>
            <div className="field-content">
              <div className="select-wrap region-select">
                <select id="region" name="region" defaultValue="" required>
                  <option value="" disabled>시도 선택</option>
                  {regions.map((region) => <option key={region} value={region}>{region}</option>)}
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
              <textarea id="memo" name="memo" maxLength={2000} aria-label="메모사항 (고장 증상 및 요청사항)" />
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
          <button className="submit-button" type="submit" disabled={products.length === 0}>
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
              <path d="M14 5H6a2 2 0 0 0-2 2v21a2 2 0 0 0 2 2h20a2 2 0 0 0 2-2V17" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
              <path d="m13 22 2-7L26 4a2.8 2.8 0 0 1 4 4L19 19l-6 3ZM23 7l4 4M15 15l4 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            신청하기
          </button>
          <p className="submission-notice" role="status">{notice}</p>
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
        <p>현재 입력 정보는 서버로 전송되거나 저장되지 않습니다. 메모에는 상담에 필요한 내용만 입력해 주세요.</p>
        <form method="dialog"><button className="dialog-close" autoFocus>닫기</button></form>
      </dialog>
    </main>
  )
}

export default App
