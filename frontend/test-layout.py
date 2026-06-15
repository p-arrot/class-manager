"""Test layout fills viewport."""
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1440, 'height': 900})

    # Login
    page.goto('http://localhost:5173/login')
    page.wait_for_load_state('networkidle')
    for inp in page.locator('input').all():
        ph = (inp.get_attribute('placeholder') or '').lower()
        if '用户' in ph: inp.fill('admin')
        if '密码' in ph: inp.fill('admin123')
    page.wait_for_timeout(300)
    for b in page.locator('button').all():
        if '登' in (b.text_content() or ''): b.click(); break
    page.wait_for_timeout(2000)
    page.wait_for_load_state('networkidle')

    # Student page
    for item in page.locator('.n-menu-item').all():
        if '学生' in (item.text_content() or ''): item.click(); break
    page.wait_for_timeout(1000)
    page.screenshot(path='/tmp/layout_student.png', full_page=True)

    # Check content height
    content = page.locator('.main-content')
    if content.count():
        box = content.bounding_box()
        print(f"Main-content: x={box['x']}, y={box['y']}, w={box['width']}, h={box['height']}")
        print(f"Viewport: 900, content reaches: {box['y'] + box['height']}")
        fills = box['y'] + box['height'] >= 800
        print(f"Fills viewport: {'YES' if fills else 'NO - only partial'}")

    # Class page
    for item in page.locator('.n-menu-item').all():
        if '班级' in (item.text_content() or ''): item.click(); break
    page.wait_for_timeout(1000)
    page.screenshot(path='/tmp/layout_class.png', full_page=True)

    browser.close()
