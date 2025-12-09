package com.adaptibot.core.condition

import com.adaptibot.common.model.Condition
import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.common.model.ImagePattern
import com.adaptibot.core.executor.actions.ConditionEvaluatorImpl
import com.adaptibot.core.executor.actions.IElementFinder
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConditionEvaluatorTest {
    
    private val elementFinder = mockk<IElementFinder>()
    private val evaluator = ConditionEvaluatorImpl(elementFinder)
    
    @Test
    fun `should evaluate ElementExists as true when element found`() {
        val identifier = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val condition = Condition.ElementExists(identifier)
        
        every { elementFinder.find(identifier) } returns Coordinate(100, 200)
        
        assertTrue(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate ElementExists as false when element not found`() {
        val identifier = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val condition = Condition.ElementExists(identifier)
        
        every { elementFinder.find(identifier) } returns null
        
        assertFalse(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate ElementNotExists correctly`() {
        val identifier = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val condition = Condition.ElementNotExists(identifier)
        
        every { elementFinder.find(identifier) } returns null
        
        assertTrue(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate AND condition with all true`() {
        val id1 = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val id2 = ElementIdentifier.ByCoordinate(Coordinate(300, 400))
        
        every { elementFinder.find(id1) } returns Coordinate(100, 200)
        every { elementFinder.find(id2) } returns Coordinate(300, 400)
        
        val condition = Condition.And(listOf(
            Condition.ElementExists(id1),
            Condition.ElementExists(id2)
        ))
        
        assertTrue(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate AND condition with one false`() {
        val id1 = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val id2 = ElementIdentifier.ByCoordinate(Coordinate(300, 400))
        
        every { elementFinder.find(id1) } returns Coordinate(100, 200)
        every { elementFinder.find(id2) } returns null
        
        val condition = Condition.And(listOf(
            Condition.ElementExists(id1),
            Condition.ElementExists(id2)
        ))
        
        assertFalse(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate OR condition with one true`() {
        val id1 = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val id2 = ElementIdentifier.ByCoordinate(Coordinate(300, 400))
        
        every { elementFinder.find(id1) } returns null
        every { elementFinder.find(id2) } returns Coordinate(300, 400)
        
        val condition = Condition.Or(listOf(
            Condition.ElementExists(id1),
            Condition.ElementExists(id2)
        ))
        
        assertTrue(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate OR condition with all false`() {
        val id1 = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val id2 = ElementIdentifier.ByCoordinate(Coordinate(300, 400))
        
        every { elementFinder.find(id1) } returns null
        every { elementFinder.find(id2) } returns null
        
        val condition = Condition.Or(listOf(
            Condition.ElementExists(id1),
            Condition.ElementExists(id2)
        ))
        
        assertFalse(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate NOT condition`() {
        val identifier = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        
        every { elementFinder.find(identifier) } returns Coordinate(100, 200)
        
        val condition = Condition.Not(Condition.ElementExists(identifier))
        
        assertFalse(evaluator.evaluate(condition))
    }
    
    @Test
    fun `should evaluate nested conditions`() {
        val id1 = ElementIdentifier.ByCoordinate(Coordinate(100, 200))
        val id2 = ElementIdentifier.ByCoordinate(Coordinate(300, 400))
        val id3 = ElementIdentifier.ByCoordinate(Coordinate(500, 600))
        
        every { elementFinder.find(id1) } returns Coordinate(100, 200)
        every { elementFinder.find(id2) } returns null
        every { elementFinder.find(id3) } returns Coordinate(500, 600)
        
        val condition = Condition.And(listOf(
            Condition.ElementExists(id1),
            Condition.Or(listOf(
                Condition.ElementExists(id2),
                Condition.ElementExists(id3)
            ))
        ))
        
        assertTrue(evaluator.evaluate(condition))
    }
}

